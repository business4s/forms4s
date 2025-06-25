package forms4s

import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

class FormElementStateSpec extends AnyFreeSpec {

  "FormState" - {
    "update" - {
      "should update a text field" in {
        // Given
        val fields @ List(field1, field2)     = List(
          FormElement.Text(randCore(), false),
          FormElement.Text(randCore(), false),
        )
        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group(randCore(), fields))
        val newValue                          = "John Doe"

        // When
        val updatedState = formState.update(FormElementUpdate.Nested(field1.core.id, FormElementUpdate.Text(newValue)))

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
          FormElement.Checkbox(randCore()),
          FormElement.Checkbox(randCore()),
        )

        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group(randCore(), fields))

        // When
        val updatedState = formState.update(FormElementUpdate.Nested(field2.core.id, FormElementUpdate.Checkbox(true)))

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
          FormElement.Select(randCore(), List("USA", "Canada", "UK")),
          FormElement.Select(randCore(), List("bar", "baz")),
        )

        val formState: FormElementState.Group = FormElementState.empty(FormElement.Group(randCore(), fields))
        val newValue                          = "UK"

        // When
        val updatedState = formState.update(FormElementUpdate.Nested(field1.core.id, FormElementUpdate.Select(newValue)))

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
        val field                             = FormElement.Text(randCore(), false)
        val fields @ List(field1, field2)     = List(
          FormElement.Group(randCore(), List(field)),
          FormElement.Group(randCore(), List(field)),
        )
        val elem                              = FormElement.Group(randCore(), fields)
        val formState: FormElementState.Group = FormElementState.empty(elem)

        // When
        val newValue     = "Main St"
        val updatedState =
          formState.update(FormElementUpdate.Nested(field2.core.id, FormElementUpdate.Nested(field.core.id, FormElementUpdate.Text(newValue))))

        // Then
        assert(
          updatedState == FormElementState.Group(
            elem,
            List(
              FormElementState.Group(
                field1,
                List(FormElementState.Text(field, "")),
              ),
              FormElementState.Group(
                field2,
                List(FormElementState.Text(field, newValue)),
              ),
            ),
          ),
        )
      }
    }
  }

  def randCore(): FormElement.Core[FormElementState] = {
    val name = Random.alphanumeric.take(8).mkString
    FormElement.Core(name, name, None, Seq())
  }
}
