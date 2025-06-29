package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate, FormStylesheet}
import tyrian.{Empty, Html, Text}
import tyrian.Html.*

class BulmaFormRenderer extends FormRenderer {

  override def renderTextInput(
      state: FormElementState.Text,
  ): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val hasError = state.errors.nonEmpty

    Html.div(`class` := "field")(
      Html.label(`class` := "label", htmlFor := name)(state.element.core.label),
      Html.div(`class` := "control")(
        Html.input(
          `type`     := "text",
          id         := name,
          Html.name  := name,
          `class`    := (if hasError then "input is-danger" else "input"),
          Html.value := state.value,
          onInput(value => FormElementUpdate.Text(value)),
        ),
      ),
      if hasError then Html.p(`class` := "help is-danger")(state.errors.mkString(", "))
      else tyrian.Empty,
    )
  }

  override def renderNumberInput(
      state: FormElementState.Number,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div(`class` := "field")(
      Html.label(`class` := "label", htmlFor := name)(state.element.core.label),
      Html.div(`class` := "control")(
        Html.input(
          `type`     := "number",
          id         := name,
          Html.name  := name,
          `class`    := "input",
          Html.value := state.value.toString,
          onInput(value => FormElementUpdate.Number(value.toDouble)),
        ),
      ),
    )
  }

  override def renderSelect(
      state: FormElementState.Select,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div(`class` := "field")(
      Html.label(`class` := "label", htmlFor := name)(state.element.core.label),
      Html.div(`class` := "control")(
        Html.div(`class` := "select")(
          tyrian.Html.select(
            id        := name,
            Html.name := name,
            onChange(value => FormElementUpdate.Select(value)),
          )(
            state.element.options.map(option =>
              tyrian.Html.option(
                value    := option,
                selected := (state.value == option),
              )(option),
            ),
          ),
        ),
      ),
    )
  }

  override def renderCheckbox(
      state: FormElementState.Checkbox,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div(`class` := "field")(
      Html.div(`class` := "control")(
        Html.label(`class` := "checkbox")(
          Html.input(
            `type`    := "checkbox",
            id        := name,
            Html.name := name,
            checked   := state.value,
            onChange(checked => FormElementUpdate.Checkbox(checked == "true")),
          ),
          Text(" "),
          Text(state.element.core.label),
        ),
      ),
    )
  }

  override def renderGroup(
      state: FormElementState.Group,
  ): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "box")(
      Html.legend(`class` := "title is-5")(state.element.core.label) ::
        state.value.map(subElement =>
          renderElement(subElement)
            .map(x => FormElementUpdate.Nested(subElement.id, x)),
        ),
    )
  }

  override def renderMultivalue(
      state: FormElementState.Multivalue,
  ): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "box")(
      Html.legend(`class` := "title is-4")(state.element.core.label) ::
        state.value.toList.zipWithIndex.flatMap { case (item, idx) =>
          List(
            renderElement(item).map(x => MultivalueUpdate(idx, x)),
            Html.div(`class` := "field is-grouped is-grouped-right")(
              Html.p(`class` := "control")(
                Html.button(
                  `class` := "button is-danger is-light is-small",
                  onClick(FormElementUpdate.MultivalueRemove(idx)),
                )("Remove"),
              ),
            ),
          )
        } ++ List(
          Html.hr(),
          Html.div(`class` := "field is-grouped is-grouped-right")(
            Html.p(`class` := "control")(
              Html.button(
                `class` := "button is-primary is-light is-small",
                onClick(FormElementUpdate.MultivalueAppend),
              )("+ Add"),
            ),
          ),
        ),
    )
  }
}

object BulmaFormRenderer extends BulmaFormRenderer
