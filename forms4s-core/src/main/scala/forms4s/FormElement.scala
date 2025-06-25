package forms4s

sealed trait FormElement {
  def core: FormElement.Core[Nothing]
}

object FormElement {

  case class Text(core: Core[FormElementState.Text], multiline: Boolean)            extends FormElement
  case class Number(core: Core[FormElementState.Number])                            extends FormElement
  case class Select(core: Core[FormElementState.Select], options: List[String])     extends FormElement
  case class Checkbox(core: Core[FormElementState.Checkbox])                        extends FormElement
  case class Group(core: Core[FormElementState.Group], elements: List[FormElement]) extends FormElement
  case class Multivalue(core: Core[FormElementState.Multivalue], item: FormElement) extends FormElement

  case class Core[-T <: FormElementState](id: String, label: String, description: Option[String], validators: Seq[Validator[T]])

  trait Validator[-In <: FormElementState] {
    def validate(in: In): Option[String]
    def executionTime: Validator.ExecutionTime
  }

  object Validator {
    enum ExecutionTime {
      case OnChange, OnSubmit // TODO we could add OnDebounce
    }
  }

}
