package forms4s.jsonschema

import forms4s.{Form, FormElement}
import org.scalatest.freespec.AnyFreeSpec
import sttp.tapir.Schema as TSchema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "convert derived schemas from Scala types" - {

    object Models {
      case class Simple(a: String, b: Int, c: Boolean)
      case class Address(street: String, city: String)
      case class User(name: String, address: Address)
    }

    "simple product → Text, Number, Checkbox (all required)" in {
      import Models.Simple
      implicit val s: TSchema[Simple] = TSchema.derived
      val aschema                     = TapirSchemaToJsonSchema(s, markOptionsAsNullable = false)
      val form                        = FormFromJsonSchema.convert(aschema)

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
      import Models.{Address, User}
      implicit val addrSchema: TSchema[Address] = TSchema.derived
      implicit val userSchema: TSchema[User]    = TSchema.derived
      val aschema                               = TapirSchemaToJsonSchema(userSchema, markOptionsAsNullable = false)
      val form                                  = FormFromJsonSchema.convert(aschema)

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
  }
}
