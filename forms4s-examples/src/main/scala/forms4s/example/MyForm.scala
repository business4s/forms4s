package forms4s.example

import sttp.tapir.Schema.annotations.{description, format, validate}
import sttp.tapir.Validator.Pattern
import sttp.tapir.generic.Configuration

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

  given Configuration = Configuration.default.withDiscriminator("type")
  sealed trait AuthMethod derives TSchema
  object AuthMethod {
    case class PasswordBased(password: String) extends AuthMethod
    case class OAuth(provider: String)         extends AuthMethod
  }

  case class User(
      @description("Full name")
      name: String,
      age: Option[Int],
      income: Double,
      emails: List[String],
      addresses: List[Address],
      preferences: UserPreferences,
      birthDate: LocalDate,
      @format("time")
      wakeupTime: OffsetTime,
      lastLogin: OffsetDateTime,
      auth: AuthMethod,
  ) derives TSchema

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    summon[TSchema[User]],
    markOptionsAsNullable = true,
  )
}
