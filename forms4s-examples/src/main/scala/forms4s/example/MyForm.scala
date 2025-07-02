package forms4s.example

import sttp.tapir.Schema.annotations.{format, validate}
import sttp.tapir.Validator.Pattern

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

object MyForm {

  import sttp.apispec.Schema as ASchema
  import sttp.tapir.Schema as TSchema
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  case class Address(
      street: String,
      @validate(Pattern("^[A-Za-z0-9\\- ]{0,10}$"))
      postalCode: String,
  ) derives TSchema

  enum Theme {
    case Light, Dark, Auto
  }
  given TSchema[Theme] = TSchema.derivedEnumeration.defaultStringBased

  case class UserPreferences(
      newsletter: Boolean,
      theme: Option[Theme],
  ) derives TSchema

  case class User(
      name: String,
      age: Option[Int],             // optional number
      income: Double,               // required number
      emails: List[String],
      addresses: List[Address],     // nested subform
      preferences: UserPreferences, // nested subform with enum and checkbox
      birthDate: LocalDate,
      @format("time")
      wakeupTime: OffsetTime,
      lastLogin: OffsetDateTime,
  ) derives TSchema

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    summon[TSchema[User]],
    markOptionsAsNullable = true,
  )

}
