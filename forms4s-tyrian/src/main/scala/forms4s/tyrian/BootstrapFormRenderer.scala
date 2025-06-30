package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate, FormStylesheet}
import tyrian.{Empty, Html, Text}
import tyrian.Html.*

class BootstrapFormRenderer extends FormRenderer {

  override def renderTextInput(
      state: FormElementState.Text,
  ): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val hasError = state.errors.nonEmpty

    Html.div(`class` := "mb-3")(
      Html.label(`class` := "form-label", htmlFor := name)(state.element.core.label),
      Html.input(
        `type`     := "text",
        id         := name,
        Html.name  := name,
        `class`    := (if hasError then "form-control is-invalid" else "form-control"),
        Html.value := state.value,
        onInput(value => FormElementUpdate.Text(value)),
      ),
      if hasError then Html.div(`class` := "invalid-feedback")(state.errors.mkString(", "))
      else Empty,
    )
  }

  override def renderNumberInput(
      state: FormElementState.Number,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div(`class` := "mb-3")(
      Html.label(`class` := "form-label", htmlFor := name)(state.element.core.label),
      Html.input(
        `type`     := "number",
        id         := name,
        Html.name  := name,
        `class`    := "form-control",
        Html.value := state.value.toString,
        onInput(value => FormElementUpdate.Number(value.toDouble)),
      ),
    )
  }

  override def renderSelect(
      state: FormElementState.Select,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id

    Html.div(`class` := "mb-3")(
      Html.label(`class` := "form-label", htmlFor := name)(state.element.core.label),
      Html.select(
        id        := name,
        Html.name := name,
        `class`   := "form-select",
        onChange(value => FormElementUpdate.Select(value)),
      )(
        state.element.options.map(option =>
          Html.option(
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

    Html.div(`class` := "form-check mb-3")(
      Html.input(
        `type`    := "checkbox",
        id        := name,
        Html.name := name,
        `class`   := "form-check-input",
        checked   := state.value,
        onChange(checked => FormElementUpdate.Checkbox(checked == "true")),
      ),
      Html.label(`class` := "form-check-label", htmlFor := name)(state.element.core.label),
    )
  }

  override def renderGroup(
      state: FormElementState.Group,
  ): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "border p-3 mb-3")(
      Html.legend(`class` := "h5")(state.element.core.label) ::
        state.value.map(subElement => renderElement(subElement).map(x => FormElementUpdate.Nested(subElement.id, x))),
    )
  }

  override def renderMultivalue(
      state: FormElementState.Multivalue,
  ): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "border p-3 mb-3")(
      Html.legend(`class` := "h4")(state.element.core.label) ::
        state.value.toList.zipWithIndex.flatMap { case (item, idx) =>
          List(
            renderElement(item).map(x => MultivalueUpdate(idx, x)),
            Html.div(`class` := "d-flex justify-content-end mb-2")(
              Html.button(
                `class` := "btn btn-danger btn-sm",
                onClick(FormElementUpdate.MultivalueRemove(idx)),
              )("Remove"),
            ),
          )
        } ++ List(
          Html.hr(),
          Html.div(`class` := "d-flex justify-content-end")(
            Html.button(
              `class` := "btn btn-primary btn-sm",
              onClick(FormElementUpdate.MultivalueAppend),
            )("+ Add"),
          ),
        ),
    )
  }
}

object BootstrapFormRenderer extends BootstrapFormRenderer
