package forms4s.example.docs

import forms4s.{FormElement, FormElementState, ToFormElem}

object GettingStarted {


  // start_doc
  case class MyForm(name: String, age: Int) derives ToFormElem
  
  val form = summon[ToFormElem[MyForm]].get

  // Create an empty form state
  val formState: FormElementState = FormElementState.empty(form)

  // Render the form (using Tyrian in this example)
  import forms4s.tyrian.BootstrapFormRenderer
  val formHtml = BootstrapFormRenderer.renderForm(formState)
    
  // Extract form data as JSON
  import forms4s.circe.*
  val formData     = formState.extractJson
  
  // Load JSON into a form
  val rebuiltState = formState.load(formData)
  // end_doc

}
