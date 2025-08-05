package forms4s

import forms4s.validation.Validator

sealed trait FormElement {
  type State
  def core: FormElement.Core[State]
}

object FormElement {

  case class Text(core: Core[String], format: Text.Format) extends FormElement {
    type State = String
  }
  object Text {

    // Lists specialized formats, but only those supported natively by the library.
    // By supported we mean some specialized rendering
    enum Format {
      case Raw
      case Multiline
      case Date
      case Time
      case DateTime
      case Email
      case Custom(name: String)
    }
  }
  case class Number(core: Core[Option[Double]], isInteger: Boolean)                 extends FormElement {
    type State = Option[Double]
  }
  case class Select(core: Core[String], options: List[String])                      extends FormElement {
    type State = String
  }
  case class Checkbox(core: Core[Boolean])                                          extends FormElement {
    type State = Boolean
  }
  case class Group(core: Core[List[FormElementState]], elements: List[FormElement]) extends FormElement {
    type State = List[FormElementState]
  }
  case class Multivalue(core: Core[Vector[FormElementState]], item: FormElement)    extends FormElement {
    type State = Vector[FormElementState]
  }

  case class Alternative(core: Core[Alternative.State], variants: Seq[FormElement], discriminator: Option[String]) extends FormElement {
    override type State = Alternative.State
  }
  object Alternative {
    case class State(selected: Int, states: Vector[FormElementState])
  }

  case class Core[-T](id: String, label: String, description: Option[String], validators: Seq[Validator[T]])

}
