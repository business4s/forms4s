package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html

case class FormState(definition: Form, values: List[FormState.Element]) {

  def update(msg: FormUpdate): FormState = {
    FormState(
      definition,
      values.map({
        case x if x.name == msg.field => x.update(msg.value)
        case x                        => x
      }),
    )
  }

  def render(
      stylesheet: FormStylesheet = FormStylesheet(),
      renderer: FormRenderer = DefaultFormRenderer,
  ): Html[FormUpdate] = {
    renderer.renderForm(this, stylesheet)
  }

}

object FormState {

  def empty(form: Form): FormState = {
    def toState(elem: FormElement): Element = elem match {
      case x: FormElement.Text     => Text(x, "")
      case x: FormElement.Select   => Select(x, x.options.headOption.getOrElse(""))
      case x: FormElement.Checkbox => Checkbox(x, false)
      case x: FormElement.Subform  => Group(x, FormState.empty(x.form))
    }
    FormState(form, form.elements.map(toState))
  }

  sealed trait Element {
    def element: FormElement
    def name: String                    = element.name
    def update(msg: FormValue): Element = {
      (this, msg) match {
        case (Text(e, _), FormValue.Text(newValue))                => Text(e, newValue)
        case (Checkbox(e, _), FormValue.Checkbox(newValue))        => Checkbox(e, newValue)
        case (Select(e, _), FormValue.Select(newValue))            => Select(e, newValue)
        case (Group(e, fields), FormValue.Nested(field, newValue)) => Group(e, fields.update(FormUpdate(field, newValue)))
        case _                                                     => ???
      }
    }
  }

  case class Text(element: FormElement.Text, value: String) extends Element

  case class Select(element: FormElement.Select, value: String) extends Element

  case class Checkbox(element: FormElement.Checkbox, value: Boolean) extends Element

  case class Group(element: FormElement.Subform, value: FormState) extends Element

}
