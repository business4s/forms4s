package forms4s.example

import cats.effect.IO
import forms4s.circe.FormStateFromJson
import forms4s.circe.FormStateToJson.extractJson
import forms4s.example.components.{CssFramework, Froms4sPlayground}
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.tyrian.*
import forms4s.{FormElementState, FormElementUpdate}
import org.scalajs.dom
import org.scalajs.dom.{URLSearchParams, document}
import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator.Pattern
import tyrian.*
import tyrian.Html.*

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.scalajs.js
import scala.scalajs.js.annotation.*

@JSExportTopLevel("TyrianApp")
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
    (Model(FormElementState.empty(form), Froms4sPlayground.empty()), Cmd.None)
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = model.page.update.andThen(x => x.copy(_1 = model.copy(page = x._1)))

  def encodeFormToQueryParam(state: FormElementState): String =
    URLEncoder.encode(state.extractJson.noSpaces, "UTF-8")

  def view(model: Model): Html[Msg] =
    div(className := "container")(
      h1("Forms4s Tyrian Example"),
      hr(),
      model.page.render,
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

case class Model(formState: FormElementState, page: Froms4sPlayground)

enum Msg {
  case FormUpdated(raw: FormElementUpdate)
  case SchemaUpdated(rawSchema: String)
  case JsonUpdated(rawJson: String)
  case Submit
  case NoOp
  case HydrateFormFromUrl(json: String)
  case FrameworkSelected(framework: CssFramework)
}
