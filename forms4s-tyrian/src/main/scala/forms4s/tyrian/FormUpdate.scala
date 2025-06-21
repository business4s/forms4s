package forms4s.tyrian

import forms4s.FormValue

// Message dispatched when a form element is updated
case class FormUpdate(field: String, value: FormValue)
