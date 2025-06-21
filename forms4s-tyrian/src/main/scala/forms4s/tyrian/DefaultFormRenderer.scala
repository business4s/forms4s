package forms4s.tyrian

import forms4s.FormValue.Nested
import forms4s.{FormState, FormStylesheet, FormValue}
import tyrian.Html
import tyrian.Html.*

class DefaultFormRenderer extends FormRenderer {
  override def renderForm(
      state: FormState,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    div(className := stylesheet.formClass)(
      state.values.map(element => renderElement(element, stylesheet)),
    )
  }

  override def renderTextInput(
      state: FormState.Text,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.name
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(name),
      input(
        `type`    := "text",
        id        := name,
        Html.name := name,
        className := stylesheet.inputClass,
        value     := state.value,
        onInput(value => FormUpdate(state.element.name, FormValue.Text(value))),
      ),
    )
  }

  override def renderSelect(
      state: FormState.Select,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.name
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(name),
      tyrian.Html.select(
        id        := name,
        Html.name := name,
        className := stylesheet.selectClass,
        onChange(value => FormUpdate(name, FormValue.Select(value))),
      )(
        state.element.options.map(option =>
          tyrian.Html.option(
            value    := option,
            selected := (state.value == option),
          )(option),
        ),
      ),
    )
  }

  override def renderCheckbox(
      state: FormState.Checkbox,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.name
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
          checked   := state.value,
          onChange(checked => FormUpdate(name, FormValue.Checkbox(checked == "true"))),
        ),
        span(name),
      ),
    )
  }

  override def renderGroup(
      state: FormState.Group,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.name
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(name) ::
        state.value.values.map(subElement =>
          renderElement(subElement, stylesheet)
            .map(x => FormUpdate(name, FormValue.Nested(x.field, x.value))),
        ),
    )
  }
}

object DefaultFormRenderer extends DefaultFormRenderer
