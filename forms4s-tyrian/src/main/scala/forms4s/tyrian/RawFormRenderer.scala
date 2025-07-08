package forms4s.tyrian

import forms4s.FormElementUpdate.MultivalueUpdate
import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.*
import tyrian.{Attr, Attribute, Empty, Html, Text}

class RawFormRenderer extends FormRenderer {

  override protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderRawInputField(state, "text")
  override protected def renderDate(state: FormElementState.Date): Html[FormElementUpdate]          = renderRawInputField(state, "date")
  override protected def renderTime(state: FormElementState.Time): Html[FormElementUpdate]          = renderRawInputField(state, "time")
  override protected def renderDateTime(state: FormElementState.DateTime): Html[FormElementUpdate]  = renderRawInputField(state, "datetime-local")
  override protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = {
    val step = Html.step := (if (state.element.isInteger) then "1" else "any")
    renderRawInputField(state, "number", step)
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
        state.value.zipWithIndex.map((subElement, idx) => renderElement(subElement).map(x => FormElementUpdate.Nested(idx, x))),
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
      additionalTags: Attr[FormElementUpdate]*,
  ): Html[FormElementUpdate] = {
    val name = state.element.core.id
    Html.div()(
      Html.label(htmlFor := name)(state.element.core.label),
      Html.input(
        List(
          `type`     := inputType,
          id         := name,
          Html.name  := name,
          Html.value := state.valueToString(state.value),
          onInput(state.emitUpdate),
        ) ++ additionalTags,
      ),
      if state.errors.nonEmpty then Html.div()(Text(state.errors.mkString(", "))) else Empty,
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    val name     = state.element.core.id
    val selected = state.value.selected

    Html.fieldset()(
      Html.legend()(state.element.core.label),
      Html.select(
        id        := name,
        Html.name := name,
        onChange(x => FormElementUpdate.AlternativeSelected(x.toInt)),
      )(
        state.element.variants.toList.zipWithIndex.map { case (elem, idx) =>
          Html.option(value := idx.toString, Html.selected := (idx == selected))(elem.core.label)
        },
      ),
      Html.div(`class` := "nested")(
        renderElement(state.value.states(selected)).map(update => FormElementUpdate.Nested(selected, update)),
      ),
    )
  }
}

object RawFormRenderer extends RawFormRenderer
