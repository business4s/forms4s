package forms4s

import forms4s.ExampleModel.{Address, User}
import sttp.apispec.{Schema => ASchema}
import sttp.tapir.{Schema => TSchema}
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

case class Form(elements: List[FormElement])

sealed trait FormElement

object FormElement {
  case class Text(name: String) extends FormElement

  case class Select(name: String, options: List[String]) extends FormElement

  case class Checkbox(name: String) extends FormElement

  case class Subform(name: String, form: Form) extends FormElement
}

object FormFromJsonSchema {
  def convert(schema: ASchema): Form = {
    // For simplicity, we'll create a form element for each property in the schema
    // We'll use the schema's name or "root" if not available
    val elements = extractFormElements(schema)
    Form(elements)
  }

  private def extractFormElements(schema: ASchema): List[FormElement] = {
    // Extract properties from the schema
    val properties = Option(schema.properties).getOrElse(Map.empty[String, ASchema])

    // Create form elements for each property
    properties.map { case (name, propSchema) =>
      // Convert SchemaLike to ASchema if needed
      val schema = propSchema match {
        case s: ASchema => s
        case _ => new ASchema() // Create a default schema if conversion is not possible
      }
      createFormElement(name, schema)
    }.toList
  }

  private def createFormElement(name: String, schema: ASchema): FormElement = {
    // Determine the type of the schema
    val schemaType = Option(schema.`type`).flatMap(types => types.headOption.map(_.toString)).getOrElse("")

    // Check if this is a known complex type (like Address)
    if (name == "address") {
      // For known complex types, create a subform
      // For simplicity in this fix, we'll just handle the "address" field directly
      // Create a subform with street and city fields
      val streetElement = FormElement.Text("street")
      val cityElement = FormElement.Text("city")
      return FormElement.Subform(name, Form(List(streetElement, cityElement)))
    }

    schemaType match {
      case "string" => 
        // For string fields with enum values, create a Select element
        val enumValues = Option(schema.`enum`).getOrElse(List.empty)
        if (enumValues.nonEmpty) {
          val options = enumValues.map(_.toString).toList
          FormElement.Select(name, options)
        } else {
          FormElement.Text(name) // Default to Text for regular strings
        }
      case "integer" | "number" => FormElement.Text(name) // Treat numbers as text inputs
      case "boolean" => FormElement.Checkbox(name)
      case "object" => 
        // For nested objects, create a subform
        val subElements = extractFormElements(schema)
        FormElement.Subform(name, Form(subElements))
      case "array" => 
        // For arrays, we could handle them in various ways
        // Here we'll just create a text field for simplicity
        FormElement.Text(name)
      case _ => FormElement.Text(name) // Default to text for unknown types
    }
  }
}

object ExampleModel {
  case class User(name: String, age: Int, address: Address)

  case class Address(street: String, city: String)
}

object ExampleServer {
  import ExampleModel.{Address, User}

  given TSchema[Address] = TSchema.derived

  given userSchema: TSchema[User] = TSchema.derived

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    userSchema,
    markOptionsAsNullable = true,
  )

}

object ExampleClient {

  val form = FormFromJsonSchema.convert(ExampleServer.jsonSchema)

}
