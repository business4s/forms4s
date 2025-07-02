package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.*
import tyrian.{Empty, Html, Text}

class RawFormRenderer extends FormRenderer {

  override protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderRawInputField(state, "text")
  override protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderRawInputField(state, "number")
  override protected def renderDate(state: FormElementState.Date): Html[FormElementUpdate]          = renderRawInputField(state, "date")
  override protected def renderTime(state: FormElementState.Time): Html[FormElementUpdate]          = renderRawInputField(state, "time")
  override protected def renderDateTime(state: FormElementState.DateTime): Html[FormElementUpdate]  = renderRawInputField(state, "datetime-local")

  override def renderSelect(
      state: FormElementState.Select,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.select(
        id        := name,
        Html.name := name,
        onChange(state.emitUpdate),
      )(
        state.element.options.map(option => Html.option(value := option, selected := (state.value == option))(option)),
      ),
    )
  }

  override def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate] = {
    val name = state.element.core.id
    Html.div()(
      Html.input(
        `type`    := "checkbox",
        id        := name,
        Html.name := name,
        checked   := state.value,
        onChange(state.emitUpdate),
      ),
      Html.label(htmlFor := name)(state.element.core.label),
    )
  }

  override def renderGroup(
      state: FormElementState.Group,
  ): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend()(state.element.core.label) ::
        state.value.map(subElement => renderElement(subElement).map(x => FormElementUpdate.Nested(subElement.id, x))),
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
            Html.button(onClick(FormElementUpdate.MultivalueRemove(idx)))("Remove"),
          )
        } ++ List(
          Html.hr(),
          Html.button(onClick(FormElementUpdate.MultivalueAppend))("+ Add"),
        ),
    )
  }

  protected def renderRawInputField(
      state: FormElementState.TextBased,
      inputType: String,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.input(
        `type`     := inputType,
        id         := name,
        Html.name  := name,
        Html.value := state.valueToString(state.value),
        onInput(state.emitUpdate),
      ),
      if state.errors.nonEmpty then Html.div()(Text(state.errors.mkString(", "))) else Empty,
    )
  }
}

object RawFormRenderer extends RawFormRenderer
