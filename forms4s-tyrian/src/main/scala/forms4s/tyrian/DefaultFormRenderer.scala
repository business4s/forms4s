package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate, FormStylesheet}
import tyrian.{Empty, Html}
import tyrian.Html.*

class DefaultFormRenderer(stylesheet: FormStylesheet) extends FormRenderer {

  override def renderTextInput(
                                state: FormElementState.Text
                              ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    val hasError = state.errors.nonEmpty

    Html.div(`class` := "field")(
      Html.label(`class` := "label", htmlFor := name)(state.element.core.label),

      Html.div(`class` := "control")(
        Html.input(
          `type` := "text",
          id := name,
          Html.name := name,
          `class` := {
            if hasError then "input is-danger"
            else "input"
          },
          Html.value := state.value,
          onInput(value => FormElementUpdate.Text(value))
        )
      ),

      if hasError then
        Html.p(`class` := "help is-danger")(state.errors.mkString(", "))
      else tyrian.Empty
    )
  }

  override def renderNumberInput(
      state: FormElementState.Number,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(state.element.core.label),
      input(
        `type`    := "number",
        id        := name,
        Html.name := name,
        className := stylesheet.inputClass,
        value     := state.value.toString,
        onInput(value => FormElementUpdate.Number(value.toDouble)),
      ),
    )
  }

  override def renderSelect(
      state: FormElementState.Select,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    div(className := stylesheet.formGroupClass)(
      label(
        htmlFor   := name,
        className := stylesheet.labelClass,
      )(state.element.core.label),
      tyrian.Html.select(
        id        := name,
        Html.name := name,
        className := stylesheet.selectClass,
        onChange(value => FormElementUpdate.Select(value)),
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
      state: FormElementState.Checkbox,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
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
          onChange(checked => FormElementUpdate.Checkbox(checked == "true")),
        ),
        span(state.element.core.label),
      ),
    )
  }

  override def renderGroup(
      state: FormElementState.Group,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(state.element.core.label) ::
        state.value.map(subElement =>
          renderElement(subElement)
            .map(x => FormElementUpdate.Nested(subElement.id, x)),
        ),
    )
  }

  override def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate] = {
    // TODO classes are bulma-specific
    fieldset(cls := "box")(
      Html.legend(cls := "title is-4")(state.element.core.label) ::
        state.value.toList.zipWithIndex.flatMap { case (item, idx) =>
          List(
            renderElement(item).map(x => MultivalueUpdate(idx, x)),
            div(cls := "field is-grouped is-grouped-right")(
              p(cls := "control")(
                button(
                  cls := "button is-danger is-light is-small",
                  onClick(FormElementUpdate.MultivalueRemove(idx)),
                )("Remove"),
              ),
            ),
          )
        } ++ List(
          hr(),
          div(cls := "field is-grouped is-grouped-right")(
            p(cls := "control")(
              button(
                cls := "button is-primary is-light is-small",
                onClick(FormElementUpdate.MultivalueAppend),
              )("+ Add"),
            ),
          ),
        ),
    )
  }
}

object DefaultFormRenderer extends DefaultFormRenderer(FormStylesheet())
