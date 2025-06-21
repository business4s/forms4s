package forms4s.tyrian

// Message dispatched when a form element is updated
case class FormUpdate(field: String, value: FormValue)
