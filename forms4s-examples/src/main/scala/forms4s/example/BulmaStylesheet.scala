package forms4s.example

import forms4s.FormStylesheet

/** Provides a Bulma stylesheet for forms.
  */
object BulmaStylesheet {

  /** A stylesheet using Bulma classes.
    */
  val stylesheet: FormStylesheet = FormStylesheet(
    formClass = "form",
    formGroupClass = "field",
    labelClass = "label",
    inputClass = "input",
    selectClass = "select",
    checkboxClass = "checkbox",
    checkboxLabelClass = "checkbox-label",
    subformClass = "box",
    subformTitleClass = "title is-5",
  )
}