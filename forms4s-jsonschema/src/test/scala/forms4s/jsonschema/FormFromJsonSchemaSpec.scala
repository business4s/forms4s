package forms4s.jsonschema

import forms4s.{FormElement, FormElementState}
import org.scalatest.freespec.AnyFreeSpec
import sttp.tapir.Schema as TSchema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "convert derived schemas from Scala types" - {

    "simple product → Text, Number, Checkbox (all required)" in {
      case class Simple(a: String, b: Int, c: Boolean) derives TSchema
      val form = getForm[Simple]()

      val expected = FormElement.Group(
        simpleCore("Simple"),
        List(
          FormElement.Text(simpleCore("a", "A"), multiline = false),
          FormElement.Number(simpleCore("b", "B")),
          FormElement.Checkbox(simpleCore("c", "C")),
        ),
      )

      assert(form == expected)
    }

    "nested case class → Subform with Text inside" in {
      case class Address(street: String) derives TSchema
      case class User(address: Address) derives TSchema
      val form = getForm[User]()

      val expected = FormElement.Group(
        simpleCore("User"),
        List(
          FormElement.Group(
            simpleCore("address", "Address"),
            List(
              FormElement.Text(simpleCore("street", "Street"), multiline = false),
            ),
          ),
        ),
      )

      assert(form == expected)
    }

    "enum → Select with options" in {
      enum Color {
        case Red, Green, Blue
      }
      given TSchema[Color] = TSchema.derivedEnumeration.defaultStringBased
      val form             = getForm[Color]()

      val expected = FormElement.Select(
        simpleCore("_$Color"),
        options = List("Blue", "Green", "Red"),
      )

      assert(form == expected)
    }

    "optional object field" in {
      enum A {
        case A1, A2
      }
      given TSchema[A] = TSchema.derivedEnumeration.defaultStringBased
      case class Interim(a: Option[A]) derives TSchema
      case class Foo(x: Interim) derives TSchema
      val form1        = getForm[Foo](nullableOptions = true)
      // this doesnt work yet, its here to document the problem
      fail("TODO")
    }

    "list → Multivalue with Text inside" in {
      val form = getForm[List[String]]()

      val expected = FormElement.Multivalue(
        simpleCore("unknown", "Unknown"),
        item = FormElement.Text(simpleCore("unknown", "Unknown"), multiline = false),
      )

      assert(form == expected)
    }

  }

  def getForm[T](nullableOptions: Boolean = false)(implicit tschema: TSchema[T]): FormElement = {
    val aschema = TapirSchemaToJsonSchema(tschema, markOptionsAsNullable = nullableOptions)
    FormFromJsonSchema.convert(aschema)
  }

  def simpleCore(name: String, label: String = null): FormElement.Core[FormElementState] =
    FormElement.Core(name, Option(label).getOrElse(name), None, Seq())
}
