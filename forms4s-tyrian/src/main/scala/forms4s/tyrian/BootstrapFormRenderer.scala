package forms4s.tyrian

import forms4s.{FormState, FormStylesheet, FormValue}
import tyrian.Html
import tyrian.Html.*

/** Bootstrap-specific implementation of FormRenderer */
object BootstrapFormRenderer extends DefaultFormRenderer {

  override def renderGroup(
      state: FormState.Group,
      stylesheet: FormStylesheet,
  ): Html[FormUpdate] = {
    val name = state.element.name
    div(className := stylesheet.subformClass)(
      h3(className := stylesheet.subformTitleClass)(name),
      div(className := "card-body")(
        state.value.values.map(subElement =>
          renderElement(subElement, stylesheet)
            .map(x => FormUpdate(name, FormValue.Nested(x.field, x.value))),
        ),
      ),
    )
  }
}
