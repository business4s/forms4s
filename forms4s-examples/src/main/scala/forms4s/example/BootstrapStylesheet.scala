package forms4s.example

import forms4s.FormStylesheet

/** Provides a Bootstrap stylesheet for forms.
  */
object BootstrapStylesheet {

  /** A stylesheet using Bootstrap classes.
    */
  val stylesheet: FormStylesheet = FormStylesheet(
    formClass = "form",
    formGroupClass = "form-group mb-3",
    labelClass = "form-label",
    inputClass = "form-control",
    selectClass = "form-select",
    checkboxClass = "form-check-input",
    checkboxLabelClass = "form-check-label",
    subformClass = "card mb-3",
    subformTitleClass = "card-header",
  )
}