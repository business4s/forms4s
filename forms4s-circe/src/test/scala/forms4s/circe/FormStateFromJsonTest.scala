package forms4s.circe

import forms4s.{FormElement, FormElementPath, FormElementState}
import forms4s.FormElement.Text.Format
import io.circe.Json
import io.circe.syntax.KeyOps
import org.scalatest.freespec.AnyFreeSpec

class FormStateFromJsonTest extends AnyFreeSpec {

  // TODO more coverage

  "date" in {
    val json                           = Json.fromString("2025-01-07")
    val elem                           = FormElement.Text(simpleCore("x"), Format.Date)
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates                        = FormStateFromJson.generateUpdates(initialState, json)
    val result                         = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected                       = FormElementState.Text(elem, "2025-01-07", Seq(), FormElementPath.Root)
    assert(result == expected)
  }

  "time" in {
    val json                           = Json.fromString("08:57:50+02:00")
    val elem                           = FormElement.Text(simpleCore("x"), Format.Time)
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates                        = FormStateFromJson.generateUpdates(initialState, json)
    val result                         = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected                       = FormElementState.Text(elem, "08:57:50+02:00", Seq(), FormElementPath.Root)
    assert(result == expected)
  }

  "datetime" in {
    val json                           = Json.fromString("2025-07-08T06:53:46.990+02:00")
    val elem                           = FormElement.Text(simpleCore("x"), Format.DateTime)
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates                        = FormStateFromJson.generateUpdates(initialState, json)
    val result                         = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected                       = FormElementState.Text(elem, "2025-07-08T06:53:46.990+02:00", Seq(), FormElementPath.Root)
    assert(result == expected)
  }

  "alternative" in {
    val json = Json.obj(
      "tpe" := "B",
      "b"   := 1,
    )

    val field       = FormElement.Number(simpleCore("a1"), true)
    val variantA    = FormElement.Group(simpleCore("A"), List(field))
    val variantB    = FormElement.Group(simpleCore("B"), List(field))
    val alternative = FormElement.Alternative(simpleCore("alt"), Seq(variantA, variantB), Some("tpe"))

    val initialState: FormElementState = FormElementState.empty(alternative)
    val updates                        = FormStateFromJson.generateUpdates(initialState, json)
    val result                         = updates.foldLeft(initialState)((s, up) => s.update(up))
    assert(
      result == FormElementState.Alternative(
        alternative,
        FormElement.Alternative.State(
          1,
          Vector(
            FormElementState
              .Group(
                variantA,
                List(FormElementState.Number(field, None, Seq(), FormElementPath.Root / "alt" / "A")),
                List(),
                FormElementPath.Root / "alt",
              ),
            FormElementState
              .Group(
                variantB,
                List(FormElementState.Number(field, None, Seq(), FormElementPath.Root / "alt" / "B")),
                List(),
                FormElementPath.Root / "alt",
              ),
          ),
        ),
        Seq(),
        FormElementPath.Root,
      ),
    )
  }

  def simpleCore(id: String) = FormElement.Core(id, "", None, Seq())

}
