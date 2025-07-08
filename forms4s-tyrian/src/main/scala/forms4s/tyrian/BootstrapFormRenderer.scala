package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate, FormStylesheet}
import tyrian.{Elem, Empty, Html, Text}
import tyrian.Html.*

class BootstrapFormRenderer extends FormRenderer {

  override def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderInput(state, "text")
  override def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderInput(state, "number")
  protected def renderDate(state: FormElementState.Date): Html[FormElementUpdate]         = renderInput(state, "date")
  protected def renderTime(state: FormElementState.Time): Html[FormElementUpdate]         = renderInput(state, "time")
  protected def renderDateTime(state: FormElementState.DateTime): Html[FormElementUpdate] = renderInput(state, "datetime-local")

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
        onChange(value => FormElementUpdate.ValueUpdate(value)),
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
        onChange(checked => FormElementUpdate.ValueUpdate(checked == "true")),
      ),
      Html.label(`class` := "form-check-label", htmlFor := name)(state.element.core.label),
    )
  }

  override def renderGroup(
      state: FormElementState.Group,
  ): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "border p-3 mb-3")(
      Html.legend(`class` := "h5")(state.element.core.label) ::
        state.value.zipWithIndex.map((subElement, idx) => renderElement(subElement).map(x => FormElementUpdate.Nested(idx, x))),
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

  protected def formWrapper(name: String, label: String)(content: Elem[FormElementUpdate]*): Html[FormElementUpdate] =
    Html.div(`class` := "mb-3")(
      (Html.label(`class` := "form-label", htmlFor := name)(label) +: content)*,
    )

  protected def errorFeedback(errors: Seq[String]): Elem[FormElementUpdate] =
    if errors.nonEmpty then Html.div(`class` := "invalid-feedback")(errors.mkString(", "))
    else tyrian.Empty

  protected def renderInput(state: FormElementState.TextBased, inputType: String): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val hasError = state.errors.nonEmpty

    formWrapper(name, state.element.core.label)(
      Html.input(
        id         := name,
        Html.name  := name,
        `class`    := (if hasError then "form-control is-invalid" else "form-control"),
        `type`     := inputType,
        Html.value := state.valueToString(state.value),
        onInput(state.emitUpdate),
      ),
      errorFeedback(state.errors),
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    val name = state.element.core.id
    val selected = state.value.selected

    Html.fieldset(`class` := "mb-3")(
      Html.legend(`class` := "form-label")(state.element.core.label),
      Html.select(
        `class` := "form-select",
        id := name,
        Html.name := name,
        onChange(x => FormElementUpdate.AlternativeSelected(x.toInt)),
      )(
        state.element.variants.toList.zipWithIndex.map { case (elem, idx) =>
          Html.option(value := idx.toString, Html.selected := (idx == selected))(elem.core.label)
        },
      ),
      Html.div(`class` := "nested mt-2")(
        renderElement(state.value.states(selected)).map(update => FormElementUpdate.Nested(selected, update)),
      ),
    )
  }

}

object BootstrapFormRenderer extends BootstrapFormRenderer
