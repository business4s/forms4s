package forms4s

import forms4s.ExampleModel.{Address, User}
import org.scalatest.freespec.AnyFreeSpec
import sttp.apispec.Schema as ASchema
import sttp.tapir.Schema as TSchema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

class FormFromJsonSchemaSpec extends AnyFreeSpec {

  "FormFromJsonSchema" - {
    "should convert a simple User schema to a form" in {
      // Given
      given addressSchema: TSchema[Address] = TSchema.derived

      given userSchema: TSchema[User] = TSchema.derived

      val jsonSchema: ASchema = TapirSchemaToJsonSchema(
        userSchema,
        markOptionsAsNullable = true
      )

      // When
      val form = FormFromJsonSchema.convert(jsonSchema)

      // Then
      assert(form.elements == List(
        FormElement.Text("name"),
        FormElement.Text("age"),
        FormElement.Subform(
          "address",
          Form(List(
            FormElement.Text("street"),
            FormElement.Text("city")
          )))
      ))
    }
  }
}
