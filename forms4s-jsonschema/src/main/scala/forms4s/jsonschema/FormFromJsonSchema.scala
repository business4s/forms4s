package forms4s.jsonschema

import cats.data.Ior
import cats.syntax.all.*
import forms4s.FormElement
import sttp.apispec.{AnySchema, ExampleMultipleValue, ExampleSingleValue, Schema as ASchema, SchemaLike, SchemaType}

import scala.annotation.tailrec

object FormFromJsonSchema {

  def convert(root: ASchema): FormElement = {
    createElement(None, root, required = true, root.$defs.getOrElse(Map()))
      .right
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
        createElement(name.some, subschema, isReq, defs)
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
  ): Ior[List[String], FormElement] = {
    schema match {
      case schema: AnySchema   =>
        val name = nameOverride.getOrElse("")
        schema match {
          case AnySchema.Anything => FormElement.Text(name, capitalizeAndSplitWords(name), None, required, multiline = true).rightIor
          case AnySchema.Nothing  => List("Nothing schema for a property is not expected").leftIor
        }
      case unresolved: ASchema =>
        unresolved.$ref match {
          case Some(value) =>
            val key = value.split("/").last
            defs.get(key) match {
              case Some(schema) => createElement(nameOverride, schema, required, defs)
              case None         => List(s"Schema for $key not found in $defs").leftIor
            }
          case None        => handleSchema(nameOverride, unresolved, required, defs)
        }

    }
  }

  private def handleSchema(nameOverride: Option[String], schema: ASchema, required: Boolean, defs: Map[String, SchemaLike]): Ior[List[String], FormElement] = {
    val name = nameOverride.getOrElse(schema.title.getOrElse("unknown"))
    val label       = schema.title.getOrElse(capitalizeAndSplitWords(name))
    val description = schema.description
    val enumOptions = schema.`enum`.getOrElse(Nil).collect {
      case ExampleSingleValue(value)    => value.toString
      case ExampleMultipleValue(values) => ??? // TODO proper error
    }

    // TODO we dont support alternative represenations (multiple schema types)
    val tpe = schema.`type`.flatMap(_.headOption)

    tpe match {
      case Some(tpe) =>
        tpe match {
          case SchemaType.Boolean                     =>
            FormElement
              .Checkbox(
                name,
                label = label,
                description = description,
                required = required,
              )
              .rightIor
          case SchemaType.Object                      =>
            extractElements(schema, schema.required.toSet, defs).map(subElems =>
              FormElement.Group(
                name,
                elements = subElems,
                label = label,
                description = description,
                required = required,
              ),
            )
          case SchemaType.Array                       =>
            schema.items
              .map(schema => createElement(None, schema, false, defs))
              .getOrElse(List(s"No items schema for array $name").leftIor)
              .map(itemElem =>
                FormElement.Multivalue(
                  id = name,
                  item = itemElem,
                  label = label,
                  description = description,
                  required = required,
                ),
              )
          case SchemaType.Number | SchemaType.Integer =>
            FormElement
              .Number(
                name,
                label = label,
                description = description,
                required = required,
              )
              .rightIor
          case SchemaType.String                      =>
            if (enumOptions.nonEmpty) {
              FormElement
                .Select(
                  name,
                  options = enumOptions,
                  label = label,
                  description = description,
                  required = required,
                )
                .rightIor
            } else {
              val maxLen      = schema.maxLength.getOrElse(100)
              val minLen      = schema.minLength.getOrElse(100)
              val formatHint  = schema.format.contains("multiline")
              val isMultiline = formatHint || maxLen > 120 || minLen > 120
              FormElement
                .Text(
                  name,
                  label = label,
                  description = description,
                  required = required,
                  multiline = isMultiline,
                )
                .rightIor
            }
          case SchemaType.Null                        => List("Null schema for a property is not expected").leftIor
        }
      case None      => List("Schema type not specified").leftIor
    }
  }

}
