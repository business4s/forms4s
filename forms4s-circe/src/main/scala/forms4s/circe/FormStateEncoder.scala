package forms4s.circe

import forms4s._
import io.circe._
import io.circe.syntax._

object FormStateEncoder {

  def encode(element: FormElementState): Json = element match {
    case FormElementState.Text(_, value)       => Json.fromString(value)
    case FormElementState.Number(_, value)     => Json.fromDoubleOrNull(value)
    case FormElementState.Checkbox(_, value)   => Json.fromBoolean(value)
    case FormElementState.Select(_, value)     => Json.fromString(value)
    case FormElementState.Group(_, values)     => Json.fromFields(values.map(elem => elem.id -> encode(elem)))
    case FormElementState.Multivalue(_, elems) => Json.fromValues(elems.map(encode))
  }

  // Extension method for easier conversion
  extension (formState: FormElementState) def extractJson: Json = encode(formState)
}
