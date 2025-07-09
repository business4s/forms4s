package forms4s

import org.scalatest.freespec.AnyFreeSpec

import java.time.{LocalDate, OffsetDateTime, OffsetTime, ZoneOffset}
import scala.util.Random

class FormElementStateSpec extends AnyFreeSpec {

  "FormElementState" - {
    "update" - {
      "text field" in {
        val field                            = FormElement.Text(randCore(), false)
        val formState: FormElementState.Text = FormElementState.empty(field)

        val newValue     = "my new value"
        val updateCmd    = formState.emitUpdate(newValue)
        val updatedState = formState.update(updateCmd)

        assert(updatedState == FormElementState.Text(field, newValue, List(), FormElementPath.Root))
      }

      "checkbox field" in {
        val field     = FormElement.Checkbox(randCore())
        val formState = FormElementState.empty(field)

        {
          val newValue     = true
          val updateCmd    = formState.emitUpdate(newValue)
          val updatedState = formState.update(updateCmd)

          val _ = assert(updatedState == FormElementState.Checkbox(field, true, List(), FormElementPath.Root))
        }

        {
          val newValue     = false
          val updateCmd    = formState.emitUpdate(newValue)
          val updatedState = formState.update(updateCmd)

          assert(updatedState == FormElementState.Checkbox(field, false, List(), FormElementPath.Root))
        }
      }

      "select field" in {
        val field     = FormElement.Select(randCore(), List("A", "B", "C"))
        val formState = FormElementState.empty(field)

        val newValue     = "C"
        val updateCmd    = formState.emitUpdate(newValue)
        val updatedState = formState.update(updateCmd)

        assert(updatedState == FormElementState.Select(field, newValue, List(), FormElementPath.Root))
      }

      "nested field" in {
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
          formState.update(FormElementUpdate.Nested(1, FormElementUpdate.Nested(0, FormElementUpdate.ValueUpdate(newValue))))

        // Then
        assert(
          updatedState == FormElementState.Group(
            elem,
            List(
              FormElementState.Group(
                field1,
                List(FormElementState.Text(field, "", Nil, FormElementPath.Root)),
                Nil,
                FormElementPath.Root,
              ),
              FormElementState.Group(
                field2,
                List(FormElementState.Text(field, newValue, Nil, FormElementPath.Root)),
                Nil,
                FormElementPath.Root,
              ),
            ),
            Nil,
            FormElementPath.Root,
          ),
        )
      }
      "time field" in {
        val field                            = FormElement.Time(randCore())
        val formState: FormElementState.Time = FormElementState.empty(field)

        val newValue     = "12:34:00"
        val updateCmd    = formState.emitUpdate(newValue)
        val updatedState = formState.update(updateCmd)

        assert(updatedState == FormElementState.Time(field, OffsetTime.of(12, 34, 0, 0, ZoneOffset.UTC), List(), FormElementPath.Root))
      }
      "date field" in {
        val field                            = FormElement.Date(randCore())
        val formState: FormElementState.Date = FormElementState.empty(field)

        val newValue     = "2024-01-23"
        val updateCmd    = formState.emitUpdate(newValue)
        val updatedState = formState.update(updateCmd)

        assert(updatedState == FormElementState.Date(field, LocalDate.of(2024, 1, 23), List(), FormElementPath.Root))
      }
      "datetime field" in {
        val field                                = FormElement.DateTime(randCore())
        val formState: FormElementState.DateTime = FormElementState.empty(field)

        val newValue     = "2025-03-01T13:45:23"
        val updateCmd    = formState.emitUpdate(newValue)
        val updatedState = formState.update(updateCmd)

        assert(
          updatedState == FormElementState.DateTime(field, OffsetDateTime.of(2025, 3, 1, 13, 45, 23, 0, ZoneOffset.UTC), List(), FormElementPath.Root),
        )
      }

    }
  }

  def randCore(): FormElement.Core[Any] = {
    val name = Random.alphanumeric.take(8).mkString
    FormElement.Core(name, name, None, Seq())
  }
}
