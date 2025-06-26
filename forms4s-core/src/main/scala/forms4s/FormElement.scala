package forms4s

sealed trait FormElement {
  type State
  def core: FormElement.Core[State]
}

object FormElement {
  type WithState[S] = FormElement { type State = S }

  case class Text(core: Core[String], multiline: Boolean)                              extends FormElement {
    type State = String
  }
  case class Number(core: Core[Double])                                                extends FormElement {
    type State = Double
  }
  case class Select(core: Core[String], options: List[String])                         extends FormElement {
    type State = String
  }
  case class Checkbox(core: Core[Boolean])                                             extends FormElement {
    type State = Boolean
  }
  case class Group(core: Core[List[FormElementState]], elements: List[FormElement]) extends FormElement {
    type State = List[FormElementState]
  }
  case class Multivalue(core: Core[Vector[FormElementState]], item: FormElement)    extends FormElement {
    type State = Vector[FormElementState]
  }

  case class Core[-T](id: String, label: String, description: Option[String], validators: Seq[Validator[T]])

  trait Validator[-T] {
    def validate(in: T): Option[String]
    def triggers: Set[Validator.ExecutionTrigger]
  }

  object Validator {
    enum ExecutionTrigger {
      case Change, Submit, Debounce, Unfocus
    }
  }

}
