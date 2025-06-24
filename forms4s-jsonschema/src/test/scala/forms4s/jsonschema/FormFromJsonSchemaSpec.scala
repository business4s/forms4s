package forms4s.jsonschema

import forms4s.FormElement.{Multivalue, Select, Subform}
import forms4s.{Form, FormElement}
import org.scalatest.freespec.AnyFreeSpec
import sttp.tapir.Schema as TSchema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "convert derived schemas from Scala types" - {

    "simple product → Text, Number, Checkbox (all required)" in {
      case class Simple(a: String, b: Int, c: Boolean) derives TSchema
      val form = getForm[Simple]()

      val expected = Form(
        List(
          FormElement.Text("a", label = "A", description = None, required = true, multiline = false),
          FormElement.Number("b", label = "B", description = None, required = true),
          FormElement.Checkbox("c", label = "C", description = None, required = true),
        ),
      )

      assert(form == expected)
    }

    "nested case class → Subform with Text inside" in {
      case class Address(street: String, city: String) derives TSchema
      case class User(name: String, address: Address) derives TSchema
      val form = getForm[User]()

      val expected = Form(
        List(
          FormElement.Text("name", label = "Name", description = None, required = true, multiline = false),
          FormElement.Subform(
            "address",
            form = Form(
              List(
                FormElement.Text("street", label = "Street", description = None, required = true, multiline = false),
                FormElement.Text("city", label = "City", description = None, required = true, multiline = false),
              ),
            ),
            label = "Address",
            description = None,
            required = true,
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
      case class WithSelect(color: Color) derives TSchema
      val form             = getForm[WithSelect]()

      val expected = Form(
        List(
          FormElement.Select(
            "color",
            options = List("Blue", "Green", "Red"),
            label = "Color",
            description = None,
            required = true,
          ),
        ),
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
      assert(form1 == Form(List(Subform("x", Form(List(Select("a", List("A1", "A2"), "_$A", None, false))), "Interim", None, true))))
    }

    "list → Multivalue with Text inside" in {
      case class ContactInfo(phones: List[String]) derives TSchema
      val form = getForm[ContactInfo]()

      val expected = Form(
        List(
          FormElement.Multivalue(
            "phones",
            item = FormElement.Text("Item", label = "Item", description = None, required = true, multiline = false),
            label = "Phones",
            description = None,
            required = true,
          ),
        ),
      )

      assert(form == expected)
    }

  }

  def getForm[T](nullableOptions: Boolean = false)(implicit tschema: TSchema[T]): Form = {
    val aschema = TapirSchemaToJsonSchema(tschema, markOptionsAsNullable = nullableOptions)
    FormFromJsonSchema.convert(aschema)
  }
}
