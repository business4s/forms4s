package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

import scala.collection.mutable

object TyrianForm {

  val defaultStylesheet: FormStylesheet = FormStylesheet()

  sealed trait FormValue

  object FormValue {
    case class Text(value: String) extends FormValue
    case class Checkbox(checked: Boolean) extends FormValue
    case class Select(value: String) extends FormValue
    case class Group(fields: Map[String, FormValue]) extends FormValue
  }

  case class FormState(values: Map[String, FormValue] = Map.empty) {

    def update(name: String, value: FormValue): FormState = {
      FormState(values + (name -> value))
    }

    def getValue(name: String): String = {
      values.get(name) match {
        case Some(FormValue.Text(value)) => value
        case Some(FormValue.Checkbox(checked)) => checked.toString
        case Some(FormValue.Select(value)) => value
        case Some(FormValue.Group(_)) => "" // Groups don't have a simple string value
        case _ => ""
      }
    }
  }

  def render[Msg](
      form: Form,
      state: FormState,
      onUpdate: (String, FormValue) => Msg,
      stylesheet: FormStylesheet = defaultStylesheet,
      renderer: FormRenderer = DefaultFormRenderer,
  ): Html[Msg] = {
    renderer.renderForm(form, state, onUpdate, stylesheet)
  }

}
