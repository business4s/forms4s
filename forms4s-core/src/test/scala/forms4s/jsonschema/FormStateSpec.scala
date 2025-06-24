package forms4s.jsonschema

import forms4s.{FormElement, FormElementState, FormElementUpdate}
import org.scalatest.freespec.AnyFreeSpec

class FormStateSpec extends AnyFreeSpec {

  "FormState" - {
    "update" - {
      "should update a text field" in {
        // Given
        val fields @ List(field1, field2)     = List(
          FormElement.Text("name", "Name", None, false, false),
          FormElement.Text("email", "Email", None, false, false),
        )
        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group("", fields, "", None, true))
        val newValue                          = "John Doe"

        // When
        val updatedState = formState.update(FormElementUpdate.Nested("name", FormElementUpdate.Text(newValue)))

        // Then
        assert(
          updatedState.values == List(
            FormElementState.Text(field1, newValue),
            FormElementState.Text(field2, ""),
          ),
        )
      }

      "should update a checkbox field" in {
        // Given
        val fields @ List(field1, field2) = List(
          FormElement.Checkbox("agree", "Agree", None, false),
          FormElement.Checkbox("xxxx", "Xxxx", None, false),
        )

        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group("", fields, "", None, true))

        // When
        val updatedState = formState.update(FormElementUpdate.Nested("xxxx", FormElementUpdate.Checkbox(true)))

        // Then
        assert(
          updatedState.values == List(
            FormElementState.Checkbox(field1, false),
            FormElementState.Checkbox(field2, true),
          ),
        )
      }

      "should update a select field" in {
        // Given
        val fields @ List(field1, field2) = List(
          FormElement.Select("country", List("USA", "Canada", "UK"), "", None, false),
          FormElement.Select("foo", List("bar", "baz"), "", None, false),
        )

        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group("", fields, "", None, true))
        val newValue  = "UK"

        // When
        val updatedState = formState.update(FormElementUpdate.Nested("country", FormElementUpdate.Select(newValue)))

        // Then
        assert(
          updatedState.values == List(
            FormElementState.Select(field1, newValue),
            FormElementState.Select(field2, "bar"),
          ),
        )
      }

      "should update a nested field" in {
        // Given
        val field                         = FormElement.Text("street", "", None, false, false)
        val fields @ List(field1, field2) = List(
          FormElement.Group("address", List(field), "", None, false),
          FormElement.Group("address2", List(field), "", None, false),
        )
        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group("", fields, "", None, true))

        // When
        val newValue     = "Main St"
        val updatedState = formState.update(FormElementUpdate.Nested("address", FormElementUpdate.Nested("street", FormElementUpdate.Text("Main St"))))

        // Then
        assert(
          updatedState.values == List(),
        )
      }
    }
  }
}
