package forms4s.example.docs

import forms4s.FormElementState
import io.circe.Json

object JsonSupportExample {
  // start_doc
  val formState: FormElementState = ???

  // Extract form data as JSON
  import forms4s.circe.*
  val formData: Json = formState.extractJson

  // Load JSON into a form
  val rebuiltState: FormElementState = formState.load(formData)
  // end_doc
}
