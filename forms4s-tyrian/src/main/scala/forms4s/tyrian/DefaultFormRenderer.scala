package forms4s.tyrian

import forms4s.FormValue.{MultivalueUpdate, Nested}
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
    val name = state.element.id
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(state.element.label),
      input(
        `type`    := "text",
        id        := name,
        Html.name := name,
        className := stylesheet.inputClass,
        value     := state.value,
        onInput(value => FormUpdate(state.element.id, FormValue.Text(value))),
      ),
    )
  }

  override def renderNumberInput(
      state: FormState.Number,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.id
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(state.element.label),
      input(
        `type`    := "number",
        id        := name,
        Html.name := name,
        className := stylesheet.inputClass,
        value     := state.value.toString,
        onInput(value => FormUpdate(state.element.id, FormValue.Number(value.toDouble))),
      ),
    )
  }

  override def renderSelect(
      state: FormState.Select,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.id
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(state.element.label),
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
    val name = state.element.id
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
        span(state.element.label),
      ),
    )
  }

  override def renderGroup(
      state: FormState.Group,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.id
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(state.element.label) ::
        state.value.values.map(subElement =>
          renderElement(subElement, stylesheet)
            .map(x => FormUpdate(name, FormValue.Nested(x.field, x.value))),
        ),
    )
  }

  override def renderMultivalue(state: FormState.Multivalue, stylesheet: FormStylesheet): Html[FormUpdate] = {
    val name = state.element.id

    // TODO classes are bulma-specific
    fieldset(cls := "box")(
      Html.legend(cls := "title is-4")(state.element.label) ::
        state.value.toList.zipWithIndex.flatMap { case (item, idx) =>
          List(
            renderElement(item, stylesheet)
              .map(x => FormUpdate(name, MultivalueUpdate(idx, x.value))),
            div(cls := "field is-grouped is-grouped-right")(
              p(cls := "control")(
                button(
                  cls := "button is-danger is-light is-small",
                  onClick(FormUpdate(name, FormValue.MultivalueRemove(idx))),
                )("Remove"),
              ),
            ),
          )
        } ++ List(
          hr(),
          div(cls := "field is-grouped is-grouped-right")(
            p(cls := "control")(
              button(
                cls := "button is-primary is-light",
                onClick(FormUpdate(name, FormValue.MultivalueAppend())),
              )("+ Add"),
            ),
          ),
        ),
    )
  }
}

object DefaultFormRenderer extends DefaultFormRenderer
