package forms4s

sealed trait FormValue

object FormValue {
  case class Text(value: String) extends FormValue

  case class Checkbox(checked: Boolean) extends FormValue

  case class Select(value: String) extends FormValue

  case class Nested(field: String, value: FormValue) extends FormValue
}