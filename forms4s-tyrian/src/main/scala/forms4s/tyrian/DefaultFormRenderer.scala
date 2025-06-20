package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

/** Default implementation of FormRenderer.
  * This implementation replicates the default rendering behavior.
  */
class DefaultFormRenderer extends FormRenderer {
  /** Renders a form as Tyrian HTML.
    */
  override def renderForm[Msg](
      form: Form,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    div(className := stylesheet.formClass)(
      form.elements.map(element => renderElement(element, state, onUpdate, "", stylesheet)),
    )
  }

  /** Renders a form element as Tyrian HTML.
    */
  override def renderElement[Msg](
      element: FormElement,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    element match {
      case text: FormElement.Text =>
        renderTextInput(text, state, onUpdate, prefix, stylesheet)
      case select: FormElement.Select =>
        renderSelect(select, state, onUpdate, prefix, stylesheet)
      case checkbox: FormElement.Checkbox =>
        renderCheckbox(checkbox, state, onUpdate, prefix, stylesheet)
      case subform: FormElement.Subform =>
        renderSubform(subform, state, onUpdate, prefix, stylesheet)
    }
  }

  /** Renders a text input element.
    */
  override def renderTextInput[Msg](
      text: FormElement.Text,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val fullName = if prefix.isEmpty then text.name else s"$prefix.${text.name}"
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := fullName,
        className := stylesheet.labelClass,
      )(text.name),
      input(
        `type`    := "text",
        id        := fullName,
        name      := fullName,
        className := stylesheet.inputClass,
        value     := state.getValue(fullName),
        onInput(value => onUpdate(fullName, value)),
      ),
    )
  }

  /** Renders a select element.
    */
  override def renderSelect[Msg](
      select: FormElement.Select,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val fullName = if prefix.isEmpty then select.name else s"$prefix.${select.name}"
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := fullName,
        className := stylesheet.labelClass,
      )(select.name),
      tyrian.Html.select(
        id        := fullName,
        name      := fullName,
        className := stylesheet.selectClass,
        onChange(value => onUpdate(fullName, value)),
      )(
        select.options.map(option =>
          tyrian.Html.option(
            value    := option,
            selected := (state.getValue(fullName) == option),
          )(option),
        ),
      ),
    )
  }

  /** Renders a checkbox element.
    */
  override def renderCheckbox[Msg](
      checkbox: FormElement.Checkbox,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val fullName = if prefix.isEmpty then checkbox.name else s"$prefix.${checkbox.name}"
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := fullName,
        className := stylesheet.checkboxLabelClass,
      )(
        input(
          `type`    := "checkbox",
          id        := fullName,
          name      := fullName,
          className := stylesheet.checkboxClass,
          checked   := (state.getValue(fullName) == "true"),
          onChange(checked => onUpdate(fullName, if checked == "true" then "true" else "false")),
        ),
        span(checkbox.name),
      ),
    )
  }

  /** Renders a subform element.
    */
  override def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val fullName = if prefix.isEmpty then subform.name else s"$prefix.${subform.name}"
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(subform.name) ::
        subform.form.elements.map(subElement => renderElement(subElement, state, onUpdate, fullName, stylesheet)),
    )
  }
}

object DefaultFormRenderer extends DefaultFormRenderer