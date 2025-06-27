package forms4s.circe

import forms4s._
import io.circe._
import io.circe.syntax._

object FormStateToJson {

  def extract(element: FormElementState): Json = element match {
    case FormElementState.Text(_, value, _)       => Json.fromString(value)
    case FormElementState.Number(_, value, _)     => Json.fromDoubleOrNull(value)
    case FormElementState.Checkbox(_, value, _)   => Json.fromBoolean(value)
    case FormElementState.Select(_, value, _)     => Json.fromString(value)
    case FormElementState.Group(_, values, _)     => Json.fromFields(values.map(elem => elem.id -> extract(elem)))
    case FormElementState.Multivalue(_, elems, _) => Json.fromValues(elems.map(extract))
  }

  // Extension method for easier conversion
  extension (formState: FormElementState) def extractJson: Json = extract(formState)
}
