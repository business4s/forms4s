package forms4s.example

import cats.effect.IO
import forms4s.{FormElementState, FormElementUpdate}
import forms4s.circe.FormStateToJson.extractJson
import forms4s.circe.FormStateFromJson
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.tyrian.*
import org.scalajs.dom
import org.scalajs.dom.URLSearchParams
import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator.Pattern
import tyrian.*
import tyrian.Html.*

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.scalajs.js.annotation.*

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

@main
def runTyrianApp(): Unit = {
  TyrianExample.launch("app")
}

object TyrianExample extends TyrianIOApp[Msg, Model] {

  def router: Location => Msg = {
    case loc: Location.Internal =>
      (for {
        params      <- loc.search
        jsonEncoded <- parseQuery(params).get("data")
        json         = URLDecoder.decode(jsonEncoded, StandardCharsets.UTF_8.name())
      } yield Msg.HydrateFormFromUrl(json)).getOrElse(Msg.NoOp)
    case _: Location.External   =>
      Msg.NoOp
  }

  def parseQuery(query: String): Map[String, String] =
    query
      .stripPrefix("?")
      .split("&")
      .flatMap { param =>
        param.split("=", 2) match {
          case Array(key, value) => Some(key -> value)
          case Array(key)        => Some(key -> "")
          case _                 => None
        }
      }
      .toMap

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val form = FormFromJsonSchema.convert(MyForm.jsonSchema)
    (Model(FormElementState.empty(form)), Cmd.None)
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.UpdateMyForm(raw: FormElementUpdate) =>
      val newState = model.formState.update(raw)
      val encoded  = encodeFormToQueryParam(newState)
      (
        model.copy(formState = newState),
        Nav.pushUrl(s"?form=$encoded"),
      )
    case Msg.Submit                               =>
      println(s"Form submitted with data: ${model.formState.extractJson}")
      (model, Cmd.None)
    case Msg.NoOp                                 =>
      (model, Cmd.None)
    case Msg.HydrateFormFromUrl(json)             =>
      val parsed       = io.circe.parser.parse(json).getOrElse(throw new Exception("invalid errr")) // TODO better error handling
      val updates      = FormStateFromJson.hydrate(model.formState, parsed)
      val newFromState = updates.foldLeft(model.formState)((acc, update) => acc.update(update))
      (model.copy(formState = newFromState), Cmd.None)
  }

  def encodeFormToQueryParam(state: FormElementState): String =
    URLEncoder.encode(state.extractJson.noSpaces, "UTF-8")

  val renderer: FormRenderer = DefaultFormRenderer(BulmaStylesheet.stylesheet)

  def view(model: Model): Html[Msg] =
    div(className := "container")(
      h1("Forms4s Tyrian Example"),
      hr(),
      div()(
        renderer.renderElement(model.formState).map(Msg.UpdateMyForm.apply),
        div(className := "form-actions")(
          button(
            onClick(Msg.Submit),
            className := "button is-primary",
          )("Submit"),
        ),
      ),
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    urlParamsSub

  def urlParamsSub: Sub[IO, Msg] =
    Sub.emit {
      val params = new URLSearchParams(dom.window.location.search)
      val json   = Option(params.get("form"))
      json match {
        case Some(value) => Msg.HydrateFormFromUrl(value)
        case None        => Msg.NoOp
      }
    }
}

case class Model(formState: FormElementState)

enum Msg {
  case UpdateMyForm(raw: FormElementUpdate)
  case Submit
  case NoOp
  case HydrateFormFromUrl(json: String)
}
