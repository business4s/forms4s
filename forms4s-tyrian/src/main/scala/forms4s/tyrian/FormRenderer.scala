package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

/** Abstraction for rendering form elements with Tyrian.
  * This allows customizing how form elements are rendered.
  */
trait FormRenderer {
  /** Renders a form as Tyrian HTML.
    * @param form
    *   The form to render.
    * @param state
    *   The current state of the form.
    * @param onUpdate
    *   A function that will be called when a field is updated.
    * @param stylesheet
    *   The stylesheet to use for rendering the form.
    * @return
    *   The HTML representation of the form.
    */
  def renderForm[Msg](
      form: Form,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg]

  /** Renders a form element as Tyrian HTML.
    * @param element
    *   The form element to render.
    * @param state
    *   The current state of the form.
    * @param onUpdate
    *   A function that will be called when a field is updated.
    * @param prefix
    *   A prefix to add to the field name (used for nested forms).
    * @param stylesheet
    *   The stylesheet to use for rendering the form element.
    * @return
    *   The HTML representation of the form element.
    */
  def renderElement[Msg](
      element: FormElement,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg]

  /** Renders a text input element.
    */
  def renderTextInput[Msg](
      text: FormElement.Text,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg]

  /** Renders a select element.
    */
  def renderSelect[Msg](
      select: FormElement.Select,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg]

  /** Renders a checkbox element.
    */
  def renderCheckbox[Msg](
      checkbox: FormElement.Checkbox,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg]

  /** Renders a subform element.
    */
  def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg]
}