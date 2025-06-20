package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

/** Abstraction for rendering form elements with Tyrian */
trait FormRenderer {
  def renderForm[Msg](
      form: Form,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  def renderElement[Msg](
      element: FormElement,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  def renderTextInput[Msg](
      text: FormElement.Text,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  def renderSelect[Msg](
      select: FormElement.Select,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  def renderCheckbox[Msg](
      checkbox: FormElement.Checkbox,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]
}
