package forms4s

case class Form(elements: List[FormElement])

sealed trait FormElement {
  def name: String
}

object FormElement {
  case class Text(name: String) extends FormElement

  case class Select(name: String, options: List[String]) extends FormElement

  case class Checkbox(name: String) extends FormElement

  case class Subform(name: String, form: Form) extends FormElement
}