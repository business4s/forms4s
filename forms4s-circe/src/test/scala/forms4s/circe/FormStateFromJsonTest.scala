package forms4s.circe

import forms4s.{FormElement, FormElementState}
import io.circe.Json
import io.circe.syntax.KeyOps
import org.scalatest.freespec.AnyFreeSpec

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

class FormStateFromJsonTest extends AnyFreeSpec {
  
  // TODO more coverage

  "date" in {
    val json = Json.fromString("2025-01-07")
    val elem = FormElement.Date(simpleCore("x"))
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates = FormStateFromJson.hydrate(initialState, json)
    val result = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected = FormElementState.Date(elem, LocalDate.parse("2025-01-07"), Seq())
    assert(result == expected)
  }

  "time" in {
    val json = Json.fromString("08:57:50+02:00")
    val elem = FormElement.Time(simpleCore("x"))
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates = FormStateFromJson.hydrate(initialState, json)
    val result = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected = FormElementState.Time(elem, OffsetTime.parse("08:57:50+02:00"), Seq())
    assert(result == expected)
  }

  "datetime" in {
    val json = Json.fromString("2025-07-08T06:53:46.990+02:00")
    val elem = FormElement.DateTime(simpleCore("x"))
    val initialState: FormElementState = FormElementState.empty(elem)
    val updates = FormStateFromJson.hydrate(initialState, json)
    val result = updates.foldLeft(initialState)((s, up) => s.update(up))
    val expected = FormElementState.DateTime(elem, OffsetDateTime.parse("2025-07-08T06:53:46.990+02:00"), Seq())
    assert(result == expected)
  }

  "alternative" in {
    val json = Json.obj(
      "tpe" := "B",
      "b"   := 1,
    )

    val field = FormElement.Number(simpleCore("a1"), true)
    val variantA    = FormElement.Group(simpleCore("A"), List(field))
    val variantB    = FormElement.Group(simpleCore("B"), List(field))
    val alternative = FormElement.Alternative(simpleCore("alt"), Seq(variantA, variantB), Some("tpe"))

    val initialState: FormElementState = FormElementState.empty(alternative)
    val updates                        = FormStateFromJson.hydrate(initialState, json)
    val result                         = updates.foldLeft(initialState)((s, up) => s.update(up))
    assert(
      result == FormElementState.Alternative(
        alternative,
        FormElement.Alternative.State(
          1,
          Vector(
            FormElementState.Group(variantA, List(FormElementState.Number(field, None, Seq())), List()),
            FormElementState.Group(variantB, List(FormElementState.Number(field, None, Seq())), List()),
          ),
        ),
        Seq(),
      ),
    )
  }

  def simpleCore(id: String) = FormElement.Core(id, "", None, Seq())

}
