package forms4s.tyrian

import forms4s.{FormState, FormStylesheet}
import tyrian.Html

/** Abstraction for rendering form elements with Tyrian */
trait FormRenderer {
  def renderForm(
      state: FormState,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderElement(
      state: FormState.Element,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = state match {
    case text: FormState.Text         => renderTextInput(text, stylesheet)
    case select: FormState.Select     => renderSelect(select, stylesheet)
    case checkbox: FormState.Checkbox => renderCheckbox(checkbox, stylesheet)
    case subform: FormState.Group     => renderGroup(subform, stylesheet)
    case number: FormState.Number     => renderNumberInput(number, stylesheet)
    case number: FormState.Multivalue => renderMultivalue(number, stylesheet)
  }

  def renderTextInput(
      state: FormState.Text,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderNumberInput(
      state: FormState.Number,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderSelect(
      state: FormState.Select,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderCheckbox(
      state: FormState.Checkbox,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderGroup(
      state: FormState.Group,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]

  def renderMultivalue(
      state: FormState.Multivalue,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate]
}
