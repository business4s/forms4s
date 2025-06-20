package forms4s.tyrian

import forms4s.{FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

/** Bootstrap-specific implementation of FormRenderer.
  * This implementation adds a card-body div around subform content for proper Bootstrap rendering.
  */
object BootstrapFormRenderer extends DefaultFormRenderer {

  /** Renders a subform element with Bootstrap-specific structure.
    * Adds a card-body div around the subform content for proper Bootstrap rendering.
    */
  override def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, String) => Msg,
      prefix: String,
      stylesheet: FormStylesheet
  ): Html[Msg] = {
    val fullName = if prefix.isEmpty then subform.name else s"$prefix.${subform.name}"
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(subform.name),
      div(className := "card-body")(
        subform.form.elements.map(subElement => renderElement(subElement, state, onUpdate, fullName, stylesheet))
      )
    )
  }
}