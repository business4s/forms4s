package forms4s.example.docs

import forms4s.tyrian.{BootstrapFormRenderer, BulmaFormRenderer}
import forms4s.{FormElement, FormElementState}

object GettingStarted {

  
  // start_doc
  case class MyForm(name: String, age: Int) derives sttp.tapir.Schema

  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema
  val jsonSchema: sttp.apispec.Schema = TapirSchemaToJsonSchema(
    summon[sttp.tapir.Schema[MyForm]],
    markOptionsAsNullable = true,
  )

  // Convert the JSON Schema to a form
  import forms4s.jsonschema.*
  val form = FormElement.fromJsonSchema(jsonSchema)

  // Create an empty form state
  val formState = FormElementState.empty(form)

  // Render the form (using Tyrian in this example)
  import forms4s.tyrian.BootstrapFormRenderer
  val formHtml = BootstrapFormRenderer.renderElement(formState)

  // Extract form data as JSON
  import forms4s.circe.*
  val formData     = formState.extractJson
  val rebuiltState = formState.load(formData)
  // end_doc

}
