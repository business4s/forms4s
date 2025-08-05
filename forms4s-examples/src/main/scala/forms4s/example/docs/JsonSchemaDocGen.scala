package forms4s.example.docs

import forms4s.jsonschema.FormFromJsonSchema
import io.circe.syntax.*
import sttp.apispec.circe.*
import sttp.apispec.{ExampleSingleValue, Schema as ASchema, SchemaType}

import scala.collection.immutable.ListMap

object JsonSchemaDocGen {

  def generateDocumentation(): String = {
    val entries       = examples.map(createTableEntry.tupled)
    val markdownTable = generateMarkdownTable(entries)
    markdownTable
  }

  val examples: List[(String, ASchema)] = List(
    "Basic text field"          -> ASchema(SchemaType.String),
    "Multiline text field"      -> ASchema(SchemaType.String).copy(format = Some("multiline")),
    "Date field"                -> ASchema(SchemaType.String).copy(format = Some("date")),
    "Time field"                -> ASchema(SchemaType.String).copy(format = Some("time")),
    "Date and time field"       -> ASchema(SchemaType.String).copy(format = Some("date-time")),
    "Email field"               -> ASchema(SchemaType.String).copy(format = Some("email")),
    "Integer number field"      -> ASchema(SchemaType.Integer),
    "Decimal number field"      -> ASchema(SchemaType.Number),
    "Boolean checkbox field"    -> ASchema(SchemaType.Boolean),
    "Array of form elements"    -> ASchema(SchemaType.Array).copy(items = Some(ASchema(SchemaType.String))),
    "Dropdown selection field"  -> ASchema(SchemaType.String).copy(
      `enum` = Some(List(ExampleSingleValue("Option 1"), ExampleSingleValue("Option 2"), ExampleSingleValue("Option 3"))),
    ),
    "Group of form elements"    -> ASchema(SchemaType.Object).copy(
      properties = ListMap(
        "field1" -> ASchema(SchemaType.String),
        "field2" -> ASchema(SchemaType.String),
      ),
    ),
    "Alternative form elements" -> ASchema.oneOf(
      List(
        new ASchema(
          `type` = Some(List(SchemaType.Object)),
          title = Some("Option 1"),
          properties = ListMap("field1" -> ASchema(SchemaType.String)),
        ),
        new ASchema(
          `type` = Some(List(SchemaType.Object)),
          title = Some("Option 2"),
          properties = ListMap("field2" -> ASchema(SchemaType.String)),
        ),
      ),
      None,
    ),
  )

  val allExamplesASSingleSchema: ASchema = ASchema(SchemaType.Object).copy(
    properties = ListMap.from(examples),
  )

  private def createTableEntry(description: String, aSchema: ASchema): (String, String, String) = {
    val schemaJson  = aSchema.asJson.spaces2
    val formElement = FormFromJsonSchema.convert(aSchema)
    val elementType = Utils.describeFormElement(formElement)
    (schemaJson, elementType, description)
  }

  private def generateMarkdownTable(examples: List[(String, String, String)]): String = {
    val headers = List("Description", "JSON Schema", "Form Element Type")
    val rows    = examples.map { case (schema, elementType, desc) =>
      List(
        desc,
        s"""<pre><code class="language-json">$schema</code></pre>""".stripMargin,
        elementType,
      )
    }

    Utils.generateHtmlTable(headers, rows)
  }

}
