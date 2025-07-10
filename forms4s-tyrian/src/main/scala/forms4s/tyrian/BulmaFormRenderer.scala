package forms4s.tyrian

import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.*
import tyrian.{Elem, Html, Text}

class BulmaFormRenderer extends FormRenderer {

  override protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderInputField(state)
  override protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderInputField(state)

  override def renderSelect(state: FormElementState.Select): Html[FormElementUpdate] = {
    Html.div(`class` := "field")(
      Html.label(`class` := "label", state.htmlFor)(state.label),
      Html.div(`class` := "control")(
        Html.div(`class` := "select")(
          Html.select(state.htmlId, state.htmlName, state.htmlOnChange)(state.htmlOptions),
        ),
      ),
    )
  }

  override def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate] = {
    Html.div(`class` := "field")(
      Html.div(`class` := "control")(
        Html.label(`class` := "checkbox")(
          Html.input(
            state.htmlType,
            state.htmlId,
            state.htmlName,
            state.htmlChecked,
            state.htmlOnChange,
          ),
          Text(state.label),
        ),
      ),
    )
  }

  override def renderGroup(state: FormElementState.Group): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "box")(
      Html.legend(`class` := "title is-5")(state.label) :: state.renderElements,
    )
  }

  override def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "box")(
      Html.legend(`class` := "title is-4")(state.label) ::
        state.elements.flatMap { elem =>
          List(
            elem.render,
            Html.div(`class` := "field is-grouped is-grouped-right")(
              Html.p(`class` := "control")(
                Html.button(`class` := "button is-danger is-light is-small", elem.htmlOnClickRemove)("Remove"),
              ),
            ),
          )
        } ++ List(
          Html.hr(),
          Html.div(`class` := "field is-grouped is-grouped-right")(
            Html.p(`class` := "control")(
              Html.button(`class` := "button is-primary is-light is-small", state.htmlOnClickAdd)("+ Add"),
            ),
          ),
        ),
    )
  }

  protected def renderInputField(state: FormElementState.SimpleInputBased): Html[FormElementUpdate] = {
    val hasError = state.errors.nonEmpty
    Html.div(`class` := "field")(
      Html.label(`class` := "label", state.htmlFor)(state.label),
      Html.div(`class` := "control")(
        Html.input(
          `class` := (if hasError then "input is-danger" else "input"),
          state.htmlType,
          state.htmlId,
          state.htmlName,
          state.htmlValue,
          state.htmlOnInput,
        ),
      ),
      renderErrors(state),
      renderDescription(state),
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    Html.fieldset()(
      Html.legend(`class` := "label")(state.label),
      Html.div(`class` := "select mb-3")(
        Html.select(
          state.htmlId,
          state.htmlName,
          state.htmlOnChange,
        )(state.htmlOptions),
      ),
      Html.div()(state.renderSelected),
    )
  }

  def renderDescription(s: FormElementState): Elem[FormElementUpdate] = {
    val description = s.element.core.description
    description.map(d => Html.p(`class` := "help")(d)).getOrElse(tyrian.Empty)
  }
  def renderErrors(s: FormElementState): Html[FormElementUpdate]      = {
    Html.div(
      s.errors.toList.map(err => Html.p(`class` := "help is-danger")(err)),
    )
  }
}

object BulmaFormRenderer extends BulmaFormRenderer
