package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate}
import tyrian.{Empty, Html, Text}
import tyrian.Html.*

class RawFormRenderer extends FormRenderer {

  override def renderTextInput(
                                state: FormElementState.Text,
                              ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.input(
        `type`     := "text",
        id         := name,
        Html.name  := name,
        Html.value := state.value,
        onInput(value => FormElementUpdate.Text(value))
      ),
      if state.errors.nonEmpty then Html.div()(Text(state.errors.mkString(", "))) else Empty
    )
  }

  override def renderNumberInput(
                                  state: FormElementState.Number,
                                ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.input(
        `type`     := "number",
        id         := name,
        Html.name  := name,
        Html.value := state.value.toString,
        onInput(value => FormElementUpdate.Number(value.toDouble))
      )
    )
  }

  override def renderSelect(
                             state: FormElementState.Select,
                           ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.select(
        id        := name,
        Html.name := name,
        onChange(value => FormElementUpdate.Select(value))
      )(
        state.element.options.map(option =>
          Html.option(
            value    := option,
            selected := (state.value == option),
          )(option)
        )
      )
    )
  }

  override def renderCheckbox(
                               state: FormElementState.Checkbox,
                             ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div()(
      Html.input(
        `type`    := "checkbox",
        id        := name,
        Html.name := name,
        checked   := state.value,
        onChange(checked => FormElementUpdate.Checkbox(checked == "true"))
      ),
      Html.label(htmlFor := name)(state.element.core.label)
    )
  }

  override def renderGroup(
                            state: FormElementState.Group,
                          ): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend()(state.element.core.label) ::
        state.value.map(subElement =>
          renderElement(subElement).map(x => FormElementUpdate.Nested(subElement.id, x))
        )
    )
  }

  override def renderMultivalue(
                                 state: FormElementState.Multivalue,
                               ): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend()(state.element.core.label) ::
        state.value.toList.zipWithIndex.flatMap { case (item, idx) =>
          List(
            renderElement(item).map(x => MultivalueUpdate(idx, x)),
            Html.button(onClick(FormElementUpdate.MultivalueRemove(idx)))("Remove")
          )
        } ++ List(
          Html.hr(),
          Html.button(onClick(FormElementUpdate.MultivalueAppend))("+ Add")
        )
    )
  }
}

object RawFormRenderer extends RawFormRenderer