import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema
import sttp.apispec.{Schema => ASchema}
import sttp.tapir.{Schema => TSchema}

case class Form(elements: List[FormElement])

sealed trait FormElement

object FormElement {
  case class Text(name: String) extends FormElement

  case class Select(name: String, options: List[String]) extends FormElement

  case class Checkbox(name: String) extends FormElement

  case class Subform(name: String, form: Form) extends FormElement
}

object Form {
  def fromJsonSchema(schema: ASchema): Form = {
    val elements = schema.`type` match {
      case Some("object") =>
        val props = schema.properties.getOrElse(Map.empty)
        props.toList.map { case (name, propSchema) =>
          propSchema.`type` match {
            case Some("string") if propSchema.`enum`.nonEmpty =>
          FormElement.Select(name, propSchema.`enum`.get.map(_.toString)) // crude conversion
            case Some("string") =>
              FormElement.Text(name)
            case Some("integer") | Some("number") =>
              FormElement.Text(name) // or introduce NumberInput
            case Some("boolean") =>
              FormElement.Checkbox(name)
            case Some("object") =>
              FormElement.Subform(name, fromJsonSchema(propSchema))
            case other =>
              throw new RuntimeException(s"Unsupported type: $other for field $name")
          }
        }
      case Some("string") =>
        List(FormElement.Text("value"))
      case Some(other) =>
        throw new RuntimeException(s"Unsupported top-level type: $other")
      case None =>
        throw new RuntimeException("Schema type is missing")
    }

    Form(elements)
  }
}

object ExampleModel {
  case class User(name: String, age: Int, address: Address)

  case class Address(street: String, city: String)
}

object ExampleServer {

  given TSchema[Address] = TSchema.derived

  given userSchema: TSchema[User] = TSchema.derived

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    userSchema,
    markOptionsAsNullable = true,
  )

}

object ExampleClient {

  val form = Form.fromJsonSchema(ExampleServer.jsonSchema)

}