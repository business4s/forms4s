package forms4s.example

import forms4s.FormStylesheet

/** Provides a Pico.css stylesheet for forms.
  */
object PicoStylesheet {

  /** A stylesheet using Pico.css classes.
    */
  val stylesheet: FormStylesheet = FormStylesheet(
    formClass = "form",
    formGroupClass = "form-group",
    labelClass = "label",
    inputClass = "input",
    selectClass = "select",
    checkboxClass = "checkbox",
    checkboxLabelClass = "checkbox-label",
    subformClass = "subform",
    subformTitleClass = "subform-title",
  )
}
