package forms4s.tyrian

import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.*
import tyrian.{Elem, Html}

class BootstrapFormRenderer extends FormRenderer {

  override def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]     = renderInput(state)
  override def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate] = renderInput(state)

  override def renderSelect(state: FormElementState.Select): Html[FormElementUpdate] = {
    Html.div(`class` := "mb-3")(
      Html.label(`class` := "form-label", state.htmlFor)(state.label),
      Html.select(
        `class` := "form-select",
        state.htmlId,
        state.htmlName,
        state.htmlOnChange,
      )(state.htmlOptions),
    )
  }

  override def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate] = {
    Html.div(`class` := "form-check mb-3")(
      Html.input(
        `class` := "form-check-input",
        state.htmlType,
        state.htmlId,
        state.htmlName,
        state.htmlChecked,
        state.htmlOnChange,
      ),
      Html.label(`class` := "form-check-label", state.htmlFor)(state.label),
    )
  }

  override def renderGroup(state: FormElementState.Group): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "border p-3 mb-3")(
      Html.legend(`class` := "h5")(state.label) :: state.renderElements,
    )
  }

  override def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "border p-3 mb-3")(
      Html.legend(`class` := "h4")(state.label) ::
        state.elements.flatMap { elem =>
          List(
            elem.render,
            Html.div(`class` := "d-flex justify-content-end mb-2")(
              Html.button(`class` := "btn btn-danger btn-sm", elem.htmlOnClickRemove)("Remove"),
            ),
          )
        } ++ List(
          Html.hr(),
          Html.div(`class` := "d-flex justify-content-end")(
            Html.button(`class` := "btn btn-primary btn-sm", state.htmlOnClickAdd)("+ Add"),
          ),
        ),
    )
  }

  protected def renderInput(state: FormElementState.SimpleInputBased): Html[FormElementUpdate] = {
    val hasError = state.errors.nonEmpty
    Html.div(`class` := "mb-3")(
      Html.label(`class` := "form-label", state.htmlFor)(state.label),
      Html.input(
        `class` := (if hasError then "form-control is-invalid" else "form-control"),
        state.htmlId,
        state.htmlName,
        state.htmlValue,
        state.htmlType,
        state.htmlOnInput,
      ),
      errorFeedback(state.errors),
      renderDescription(state),
    )
  }

  override def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate] = {
    Html.fieldset(`class` := "mb-3")(
      Html.legend(`class` := "form-label")(state.label),
      Html.select(
        `class` := "form-select",
        state.htmlId,
        state.htmlName,
        state.htmlOnChange,
      )(state.htmlOptions),
      Html.div(`class` := "nested mt-2")(state.renderSelected),
    )
  }

  protected def errorFeedback(errors: Seq[String]): Elem[FormElementUpdate] =
    if errors.nonEmpty then Html.div(`class` := "invalid-feedback")(errors.mkString(", "))
    else tyrian.Empty

  protected def renderDescription(s: FormElementState): Elem[FormElementUpdate] = {
    s.element.core.description
      .map(desc => Html.small(`class` := "form-text ")(desc))
      .getOrElse(tyrian.Empty)
  }

}

object BootstrapFormRenderer extends BootstrapFormRenderer
