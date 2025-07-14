package forms4s.example.docs

import forms4s.FormElement

object JsonSchemaSupportExample {
  // start_doc
  val jsonSchema: sttp.apispec.Schema = ???

  // Convert the JSON Schema to a form
  import forms4s.jsonschema.*
  val form = FormElement.fromJsonSchema(jsonSchema)
  // end_doc
}
