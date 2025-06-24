package forms4s.jsonschema

import forms4s.FormElement
import org.scalatest.freespec.AnyFreeSpec
import sttp.tapir.Schema as TSchema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "convert derived schemas from Scala types" - {

    "simple product → Text, Number, Checkbox (all required)" in {
      case class Simple(a: String, b: Int, c: Boolean) derives TSchema
      val form = getForm[Simple]()

      val expected = FormElement.Group(
        "Simple",
        List(
          FormElement.Text("a", label = "A", description = None, required = true, multiline = false),
          FormElement.Number("b", label = "B", description = None, required = true),
          FormElement.Checkbox("c", label = "C", description = None, required = true),
        ),
        label = "Simple",
        description = None,
        required = true,
      )

      assert(form == expected)
    }

    "nested case class → Subform with Text inside" in {
      case class Address(street: String) derives TSchema
      case class User(address: Address) derives TSchema
      val form = getForm[User]()

      val expected = FormElement.Group(
        "User",
        List(
          FormElement.Group(
            "address",
            List(
              FormElement.Text("street", label = "Street", description = None, required = true, multiline = false),
            ),
            label = "Address",
            description = None,
            required = true,
          ),
        ),
        label = "User",
        description = None,
        required = true,
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
        "_$Color",
        options = List("Blue", "Green", "Red"),
        label = "_$Color",
        description = None,
        required = true,
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

      val expected = List(
        FormElement.Multivalue(
          "phones",
          item = FormElement.Text("Item", label = "Item", description = None, required = true, multiline = false),
          label = "Phones",
          description = None,
          required = true,
        ),
      )

      assert(form == expected)
    }

  }

  def getForm[T](nullableOptions: Boolean = false)(implicit tschema: TSchema[T]): FormElement = {
    val aschema = TapirSchemaToJsonSchema(tschema, markOptionsAsNullable = nullableOptions)
    FormFromJsonSchema.convert(aschema)
  }
}
