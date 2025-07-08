package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.*
import tyrian.{Html, Text}

class BulmaFormRenderer extends FormRenderer {

  override protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderInputField(state, "text")
  override protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderInputField(state, "number")
  override protected def renderDate(state: FormElementState.Date): Html[FormElementUpdate]          = renderInputField(state, "date")
  override protected def renderTime(state: FormElementState.Time): Html[FormElementUpdate]          = renderInputField(state, "time")
  override protected def renderDateTime(state: FormElementState.DateTime): Html[FormElementUpdate]  = renderInputField(state, "datetime-local")

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
            onChange(state.emitUpdate),
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
            onChange(state.emitUpdate),
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
        state.value.zipWithIndex.map((subElement, idx) =>
          renderElement(subElement)
            .map(x => FormElementUpdate.Nested(idx, x)),
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

  protected def renderInputField(
      state: FormElementState.TextBased,
      inputType: String,
  ): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val label    = state.element.core.label
    val hasError = state.errors.nonEmpty

    Html.div(`class` := "field")(
      Html.label(`class` := "label", htmlFor := name)(label),
      Html.div(`class` := "control")(
        Html.input(
          `type`     := inputType,
          id         := name,
          Html.name  := name,
          `class`    := (if hasError then "input is-danger" else "input"),
          Html.value := state.valueToString(state.value),
          onInput(state.emitUpdate),
        ),
      ),
      if hasError then Html.p(`class` := "help is-danger")(state.errors.mkString(", "))
      else tyrian.Empty,
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val selected = state.value.selected

    Html.fieldset()(
      Html.legend(`class` := "label")(state.element.core.label),
      Html.div(`class` := "select mb-3")(
        Html.select(
          id        := name,
          Html.name := name,
          onChange(x => FormElementUpdate.AlternativeSelected(x.toInt)),
        )(
          state.element.variants.toList.zipWithIndex.map { case (elem, idx) =>
            Html.option(value := idx.toString, Html.selected := (idx == selected))(elem.core.label)
          },
        ),
      ),
      Html.div()(
        renderElement(state.value.states(selected)).map(update => FormElementUpdate.Nested(selected, update)),
      ),
    )
  }
}

object BulmaFormRenderer extends BulmaFormRenderer
