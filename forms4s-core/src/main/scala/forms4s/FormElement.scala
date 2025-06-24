package forms4s

sealed trait FormElement {
  def id: String

  def label: String

  def description: Option[String]
  def required: Boolean
}

object FormElement {

  case class Text(
      id: String,
      label: String,
      description: Option[String],
      required: Boolean,
      multiline: Boolean,
  ) extends FormElement

  case class Number(
      id: String,
      label: String,
      description: Option[String],
      required: Boolean,
  ) extends FormElement

  case class Select(
      id: String,
      options: List[String],
      label: String,
      description: Option[String],
      required: Boolean,
  ) extends FormElement

  case class Checkbox(
      id: String,
      label: String,
      description: Option[String],
      required: Boolean,
  ) extends FormElement

  case class Group(
      id: String,
      elements: List[FormElement],
      label: String,
      description: Option[String],
      required: Boolean,
  ) extends FormElement

  case class Multivalue(
      id: String,
      item: FormElement,
      label: String,
      description: Option[String],
      required: Boolean,
  ) extends FormElement

}
