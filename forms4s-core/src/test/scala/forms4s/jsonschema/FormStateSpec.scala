package forms4s.jsonschema

import forms4s.{Form, FormElement, FormState, FormValue}
import org.scalatest.freespec.AnyFreeSpec

class FormStateSpec extends AnyFreeSpec {

  "FormState" - {
    "update" - {
      "should update a text field" in {
        // Given
        val fields @ List(field1, field2) = List(
          FormElement.Text("name", "Name", None, false, false),
          FormElement.Text("email", "Email", None, false, false),
        )
        val formState                     = FormState.empty(Form(fields))
        val newValue                      = "John Doe"

        // When
        val updatedState = formState.update("name", FormValue.Text(newValue))

        // Then
        assert(
          updatedState.values == List(
            FormState.Text(field1, newValue),
            FormState.Text(field2, ""),
          ),
        )
      }

      "should update a checkbox field" in {
        // Given
        val fields @ List(field1, field2) = List(
          FormElement.Checkbox("agree", "Agree", None, false),
          FormElement.Checkbox("xxxx", "Xxxx", None, false),
        )

        val formState = FormState.empty(Form(fields))

        // When
        val updatedState = formState.update("xxxx", FormValue.Checkbox(true))

        // Then
        assert(
          updatedState.values == List(
            FormState.Checkbox(field1, false),
            FormState.Checkbox(field2, true),
          ),
        )
      }

      "should update a select field" in {
        // Given
        val fields @ List(field1, field2) = List(
          FormElement.Select("country", List("USA", "Canada", "UK"), "", None, false),
          FormElement.Select("foo", List("bar", "baz"), "", None, false),
        )

        val formState = FormState.empty(Form(fields))
        val newValue  = "UK"

        // When
        val updatedState = formState.update("country", FormValue.Select(newValue))

        // Then
        assert(
          updatedState.values == List(
            FormState.Select(field1, newValue),
            FormState.Select(field2, "bar"),
          ),
        )
      }

      "should update a nested field" in {
        // Given
        val field                         = FormElement.Text("street", "", None, false, false)
        val fields @ List(field1, field2) = List(
          FormElement.Subform("address", Form(List(field)), "", None, false),
          FormElement.Subform("address2", Form(List(field)), "", None, false),
        )
        val formState                     = FormState.empty(Form(fields))

        // When
        val newValue     = "Main St"
        val updatedState = formState.update("address", FormValue.Nested("street", FormValue.Text("Main St")))

        // Then
        assert(
          updatedState.values == List(
            FormState.Group(
              field1,
              FormState(
                Form(List(field)),
                List(
                  FormState.Text(field, newValue),
                ),
              ),
            ),
            FormState.Group(
              field2,
              FormState(
                Form(List(field)),
                List(
                  FormState.Text(field, ""),
                ),
              ),
            ),
          ),
        )
      }
    }
  }
}
