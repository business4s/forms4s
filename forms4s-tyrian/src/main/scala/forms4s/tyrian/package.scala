package forms4s.tyrian

import forms4s.{FormState, FormStylesheet}
import tyrian.Html

extension (formState: FormState)
  def render(
      stylesheet: FormStylesheet = FormStylesheet(),
      renderer: FormRenderer = DefaultFormRenderer,
  ): Html[FormUpdate] = {
    renderer.renderForm(formState, stylesheet)
  }
