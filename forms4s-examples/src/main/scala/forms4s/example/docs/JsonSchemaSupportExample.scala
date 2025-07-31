package forms4s.example.docs

import forms4s.FormElement

object JsonSchemaSupportExample {
  // start_doc
  // This is just an example, schema can be acquired or produced in any way
  case class MyForm(name: String, age: Int) derives sttp.tapir.Schema
  val jsonSchema: sttp.apispec.Schema =
    sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema(
      summon[sttp.tapir.Schema[MyForm]],
      markOptionsAsNullable = true,
    )

  // Convert the JSON Schema to a form
  import forms4s.jsonschema.*
  val form = FormElement.fromJsonSchema(jsonSchema)
  // end_doc
}
