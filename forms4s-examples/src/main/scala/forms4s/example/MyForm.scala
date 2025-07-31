package forms4s.example

import java.time.LocalDate

object MyForm {

  import sttp.apispec.Schema as ASchema
  import sttp.tapir.Schema as TSchema
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  case class Address(
      street: String,
      city: String,
  ) derives TSchema

  enum Theme {
    case Light, Dark
  }

  given TSchema[Theme] = TSchema.derivedEnumeration.defaultStringBased

  case class UserPreferences(
      newsletter: Boolean,
      theme: Option[Theme],
  ) derives TSchema

  case class User(
      name: String,
      birthDate: LocalDate,
      address: Address,
      preferences: UserPreferences,
      emails: List[String],
  ) derives TSchema

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    summon[TSchema[User]],
    markOptionsAsNullable = true,
  )
}
