package forms4s.circe

import forms4s._
import io.circe._
import io.circe.syntax._

object FormStateEncoder {

  def encodeFormState(formState: FormState): Json = {
    val valueMap = formState.values.map { element =>
      element.name -> encodeElement(element)
    }.toMap

    Json.fromFields(valueMap)
  }

  private def encodeElement(element: FormState.Element): Json = element match {
    case FormState.Text(_, value) => Json.fromString(value)
    case FormState.Number(_, value) => Json.fromDoubleOrNull(value)
    case FormState.Checkbox(_, value) => Json.fromBoolean(value)
    case FormState.Select(_, value) => Json.fromString(value)
    case FormState.Group(_, subState) => encodeFormState(subState)
  }

  // Extension method for easier conversion
  extension (formState: FormState)
    def extractJson: Json = encodeFormState(formState)
}
