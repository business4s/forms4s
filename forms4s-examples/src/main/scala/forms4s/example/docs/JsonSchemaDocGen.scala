package forms4s.example.docs

import forms4s.jsonschema.FormFromJsonSchema
import io.circe.syntax.*
import sttp.apispec.circe.*
import sttp.apispec.{ExampleSingleValue, SchemaType, Schema as ASchema}

import java.io.{File, PrintWriter}
import scala.collection.immutable.ListMap
import scala.util.Using

object JsonSchemaDocGen {
  
  def generateDocumentation(
      outputPath: String = "forms4s-examples/src/test/resources/docs/json-schema-form-elements.md",
  ): Unit = {
    val directory = new File(outputPath).getParentFile
    val _         = directory.mkdirs()

    val entries       = examples.map(createTableEntry.tupled)
    val markdownTable  = generateMarkdownTable(entries)

    val _ = Using(new PrintWriter(outputPath))(_.write(markdownTable))
    println(s"Documentation generated at $outputPath")
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

  def createTableEntry(description: String, aSchema: ASchema): (String, String, String) = {
    val schemaJson  = aSchema.asJson.spaces2
    val formElement = FormFromJsonSchema.convert(aSchema)
    val elementType = Utils.describeFormElement(formElement)
    (schemaJson, elementType, description)
  }

  def generateMarkdownTable(examples: List[(String, String, String)]): String = {
    val rows = examples
      .map { case (schema, elementType, desc) =>
        s"""<tr>
           |<td>$desc</td>
           |<td><pre><code class="language-json">$schema
           |</code></pre></td>
           |<td>$elementType</td>
           |</tr>""".stripMargin
      }
      .mkString("\n")

    s"""<table>
       |<thead>
       |<tr>
       |<th>Description</th>
       |<th>JSON Schema</th>
       |<th>Form Element Type</th>
       |</tr>
       |</thead>
       |<tbody>
       |$rows
       |</tbody>
       |</table>""".stripMargin
  }
  
  def main(args: Array[String]): Unit = generateDocumentation()
}
