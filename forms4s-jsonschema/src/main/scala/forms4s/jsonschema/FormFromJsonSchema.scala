package forms4s.jsonschema

import cats.data.Ior
import cats.syntax.all.*
import forms4s.FormElement
import forms4s.FormElement.Text.Format
import forms4s.validation.{FormatValidator, RegexValidator, Validator}
import sttp.apispec.{AnySchema, ExampleMultipleValue, ExampleSingleValue, Schema as ASchema, SchemaLike, SchemaType}

import java.time.Duration
import java.util.UUID
import scala.annotation.tailrec
import scala.util.Try
import scala.util.matching.Regex

object FormFromJsonSchema {

  def convert(root: ASchema): FormElement = {
    createElement(None, root, required = true, root.$defs.getOrElse(Map()), None).right
      .getOrElse(???) // TODO errors
  }

  type Result = Ior[List[String], List[FormElement]]

  /** Recursively extract all properties as FormElements */
  private def extractElements(
      schema: ASchema,
      requiredFields: Set[String],
      defs: Map[String, SchemaLike],
  ): Result = {
    schema.properties.toList
      .map({ case (name, subschema) =>
        val isReq = requiredFields.contains(name)
        createElement(name.some, subschema, isReq, defs, None)
      })
      .map(_.map(List(_)))
      .combineAllOption
      .getOrElse(Ior.left(List("No properties found")))
  }

  private def capitalizeAndSplitWords(str: String): String =
    str.split("(?=\\p{Upper})").map(_.capitalize).mkString(" ")

  @tailrec
  private def createElement(
      nameOverride: Option[String],
      schema: SchemaLike,
      required: Boolean,
      defs: Map[String, SchemaLike],
      parentDiscriminator: Option[String],
  ): Ior[List[String], FormElement] = {
    schema match {
      case schema: AnySchema   =>
        val name = nameOverride.getOrElse("")
        schema match {
          case AnySchema.Anything =>
            FormElement.Text(FormElement.Core(name, capitalizeAndSplitWords(name), None, Seq()), Format.Raw).rightIor
          case AnySchema.Nothing  => List("Nothing schema for a property is not expected").leftIor
        }
      case unresolved: ASchema =>
        unresolved.$ref match {
          case Some(value) =>
            val key = value.split("/").last
            defs.get(key) match {
              case Some(schema) => createElement(nameOverride, schema, required, defs, parentDiscriminator)
              case None         => List(s"Schema for $key not found in $defs").leftIor
            }
          case None        => handleSchema(nameOverride, unresolved, defs, parentDiscriminator)
        }
    }
  }

  private def handleSchema(
      nameOverride: Option[String],
      schema: ASchema,
      defs: Map[String, SchemaLike],
      parentDiscriminator: Option[String],
  ): Ior[List[String], FormElement] = {
    val name                                   = nameOverride.getOrElse(schema.title.getOrElse("unknown"))
    val label                                  = schema.title.getOrElse(capitalizeAndSplitWords(name))
    def core[T](validators: Seq[Validator[T]]) = FormElement.Core(name, label, schema.description, validators)

    if (schema.oneOf.nonEmpty) {
      val discriminator = schema.discriminator.map(_.propertyName)
      schema.oneOf
        .traverse { subSchema => createElement(None, subSchema, required = true, defs, discriminator) } // TODO required seems fishy
        .map { subElems => FormElement.Alternative(core(Seq()), subElems, discriminator) }
    } else {
      val enumOptions = schema.`enum`.getOrElse(Nil).collect {
        case ExampleSingleValue(value)    => value.toString
        case ExampleMultipleValue(values) => ??? // TODO proper error
      }

      // TODO we dont support alternative representations (multiple schema types)
      val tpe = schema.`type`.flatMap(_.headOption)

      tpe match {
        case Some(tpe) =>
          tpe match {
            case SchemaType.Boolean => FormElement.Checkbox(core(Seq())).rightIor
            case SchemaType.Object  =>
              extractElements(schema, schema.required.toSet, defs)
                .map(subElems => FormElement.Group(core(Seq()), subElems.filter(x => !parentDiscriminator.contains(x.core.id))))
            case SchemaType.Array   =>
              schema.items
                .map(schema => createElement(None, schema, false, defs, None))
                .getOrElse(List(s"No items schema for array $name").leftIor)
                .map(itemElem => FormElement.Multivalue(core(Seq()), itemElem))
            case SchemaType.Number  => FormElement.Number(core(Seq()), isInteger = false).rightIor
            case SchemaType.Integer => FormElement.Number(core(Seq()), isInteger = true).rightIor
            case SchemaType.String  =>
              if (enumOptions.nonEmpty) {
                FormElement.Select(core(Seq()), enumOptions).rightIor
              } else {
                val format = schema.format.map(_.toLowerCase)
                val maxLen = schema.maxLength.getOrElse(100)
                val minLen = schema.minLength.getOrElse(100)

                // TODO validators
                val (validators, tFormat) = format match {
                  case Some("date")         => Seq() -> Format.Date
                  case Some("multiline")    => Seq() -> Format.Multiline
                  case Some("time")         => Seq() -> Format.Time
                  case Some("date-time")    => Seq() -> Format.DateTime
                  case Some("email")        =>
                    Seq(FormatValidator("ISO 8601 duration", x => Try(Duration.parse(x)).isSuccess, Some("PT20H10M"))) -> Format.Email
                  case Some(x @ "uuid")     =>
                    Seq(FormatValidator("UUIDv4", x => Try(UUID.fromString(x)).isSuccess, Some("d3399597-a3b6-4813-ac0a-23bb84a95e11"))) -> Format
                      .Custom(x)
                  case Some(x @ "duration") => Seq() -> Format.Custom(x)
                  case Some(x)              => Seq() -> Format.Custom(x)
                  case None                 =>
                    val isMultiline = maxLen > 120 || minLen > 120
                    val format      = if (isMultiline) Format.Multiline else Format.Raw
                    Seq() -> format
                }
                val patternOpt            = schema.pattern.map(p => RegexValidator(Regex(p.value)))
                FormElement.Text(core(validators ++ patternOpt.toList), tFormat).rightIor
              }

            case SchemaType.Null => List("Null schema for a property is not expected").leftIor
          }
        case None      => List("Schema type not specified").leftIor
      }
    }
  }

}
