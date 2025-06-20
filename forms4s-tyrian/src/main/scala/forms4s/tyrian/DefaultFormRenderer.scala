package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

class DefaultFormRenderer extends FormRenderer {
  override def renderForm[Msg](
      form: Form,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    div(className := stylesheet.formClass)(
      form.elements.map(element => renderElement(element, state, onUpdate, stylesheet)),
    )
  }

  override def renderElement[Msg](
      element: FormElement,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    element match {
      case text: FormElement.Text =>
        renderTextInput(text, state, onUpdate, stylesheet)
      case select: FormElement.Select =>
        renderSelect(select, state, onUpdate, stylesheet)
      case checkbox: FormElement.Checkbox =>
        renderCheckbox(checkbox, state, onUpdate, stylesheet)
      case subform: FormElement.Subform =>
        renderSubform(subform, state, onUpdate, stylesheet)
    }
  }

  override def renderTextInput[Msg](
      text: FormElement.Text,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val name = text.name
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(text.name),
      input(
        `type`    := "text",
        id        := name,
        Html.name := name,
        className := stylesheet.inputClass,
        value     := state.getValue(name),
        onInput(value => onUpdate(name, TyrianForm.FormValue.Text(value))),
      ),
    )
  }

  override def renderSelect[Msg](
      select: FormElement.Select,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val name = select.name
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(select.name),
      tyrian.Html.select(
        id        := name,
        Html.name := name,
        className := stylesheet.selectClass,
        onChange(value => onUpdate(name, TyrianForm.FormValue.Select(value))),
      )(
        select.options.map(option =>
          tyrian.Html.option(
            value    := option,
            selected := (state.getValue(name) == option),
          )(option),
        ),
      ),
    )
  }

  override def renderCheckbox[Msg](
      checkbox: FormElement.Checkbox,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val name = checkbox.name
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.checkboxLabelClass,
      )(
        input(
          `type`    := "checkbox",
          id        := name,
          Html.name := name,
          className := stylesheet.checkboxClass,
          checked   := (state.getValue(name) == "true"),
          onChange(checked => onUpdate(name, TyrianForm.FormValue.Checkbox(checked == "true"))),
        ),
        span(checkbox.name),
      ),
    )
  }

  override def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(subform.name) ::
        subform.form.elements.map(subElement => renderElement(subElement, state, onUpdate, stylesheet)),
    )
  }
}

object DefaultFormRenderer extends DefaultFormRenderer
