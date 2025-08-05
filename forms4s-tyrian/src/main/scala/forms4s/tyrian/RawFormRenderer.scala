package forms4s.tyrian

import forms4s.{FormElementState, FormElementUpdate}
import tyrian.*

class RawFormRenderer extends FormRenderer {

  override protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderRawInputField(state)
  override protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderRawInputField(state, state.htmlStep)

  override def renderSelect(state: FormElementState.Select): Html[FormElementUpdate] = {
    Html.div()(
      Html.label(state.htmlFor)(state.label),
      Html.select(state.htmlId, state.htmlName, state.htmlOnChange)(state.htmlOptions),
    )
  }

  override def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate] = {
    Html.div()(
      Html.input(
        state.htmlType,
        state.htmlId,
        state.htmlName,
        state.htmlChecked,
        state.htmlOnChange,
      ),
      Html.label(state.htmlFor)(state.label),
    )
  }

  override def renderGroup(state: FormElementState.Group): Html[FormElementUpdate] = {
    Html.fieldset(
      Html.legend()(state.label) :: state.renderElements,
    )
  }

  override def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend()(state.label) ::
        state.elements.flatMap { elem =>
          List(
            elem.render,
            Html.button(elem.htmlOnClickRemove)("Remove"),
          )
        } ++ List(
          Html.hr(),
          Html.button(state.htmlOnClickAdd)("+ Add"),
        ),
    )
  }

  protected def renderRawInputField(
      state: FormElementState.SimpleInputBased,
      additionalTags: Attr[FormElementUpdate]*,
  ): Html[FormElementUpdate] = {
    val errorsId    = (state.path / "_errors").asHtmlId
    val ariaInvalid = if state.errors.nonEmpty then Attribute("aria-invalid", "true") else EmptyAttribute
    Html.div()(
      Html.label(state.htmlFor)(state.element.core.label),
      Html.input(
        List(
          state.htmlType,
          state.htmlId,
          state.htmlName,
          state.htmlValue,
          state.htmlOnInput,
          Attribute("aria-describedby", errorsId),
          ariaInvalid,
        ) ++ additionalTags,
      ),
      renderDescription(state),
      if state.errors.nonEmpty then Html.small(Html.id := errorsId)(Text(state.errors.mkString(", "))) else Empty,
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend()(state.label),
      Html.select(state.htmlId, state.htmlName, state.htmlOnChange)(state.htmlOptions),
      Html.div(state.renderSelected),
    )
  }

  protected def renderDescription(s: FormElementState): Elem[FormElementUpdate] = {
    val descOpt = s.element.core.description
    descOpt.map(desc => Html.small(desc)).getOrElse(Empty)
  }
}

object RawFormRenderer extends RawFormRenderer
