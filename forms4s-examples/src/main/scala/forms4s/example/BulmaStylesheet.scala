package forms4s.example

import forms4s.FormStylesheet

object BulmaStylesheet {

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
