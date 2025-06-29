package forms4s.example

import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator.Pattern

object MyForm {

  import sttp.apispec.Schema as ASchema
  import sttp.tapir.Schema as TSchema
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  case class Address(
      street: String,
      city: String,
      @validate(Pattern("^[A-Za-z0-9\\- ]{0,10}$"))
      postalCode: String,
      country: String,
      notes: Option[String], // long multiline optional text
  ) derives TSchema

  enum Theme {
    case Light, Dark, Auto
  }
  given TSchema[Theme] = TSchema.derivedEnumeration.defaultStringBased

  case class UserPreferences(
      newsletter: Boolean,
      theme: Option[Theme], // enum: "light", "dark", "auto"
  ) derives TSchema

  case class User(
      name: String,
      age: Option[Int],          // optional number
      income: Double,            // required number
      biography: Option[String], // long multiline optional text
      emails: List[String],
      addresses: List[Address],  // nested subform
      preferences: UserPreferences, // nested subform with enum and checkbox
  ) derives TSchema

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    summon[TSchema[User]],
    markOptionsAsNullable = true,
  )

}
