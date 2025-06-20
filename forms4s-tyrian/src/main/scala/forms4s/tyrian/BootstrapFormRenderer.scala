package forms4s.tyrian

import forms4s.{FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

/** Bootstrap-specific implementation of FormRenderer */
object BootstrapFormRenderer extends DefaultFormRenderer {

  override def renderSubform[Msg](
      subform: FormElement.Subform,
      state: TyrianForm.FormState,
      onUpdate: (String, TyrianForm.FormValue) => Msg,
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
