package forms4s.example

import cats.effect.IO
import forms4s.circe.FormStateToJson.extractJson
import forms4s.example.components.{CssFramework, DatatableMsg, DatatablePlayground, Froms4sPlayground}
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.{FormElementState, FormElementUpdate}
import org.scalajs.dom
import org.scalajs.dom.URLSearchParams
import tyrian.*
import tyrian.Html.*
import tyrian.Nav

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.scalajs.js.annotation.*
import sttp.apispec.Schema as ASchema

enum Tab {
  case Forms
  case Datatable
}

object Tab {
  def fromHash(hash: String): Option[Tab] = hash.stripPrefix("#") match {
    case "forms"     => Some(Tab.Forms)
    case "datatable" => Some(Tab.Datatable)
    case ""          => Some(Tab.Forms)
    case _           => None
  }

  def toHash(tab: Tab): String = tab match {
    case Tab.Forms     => "#forms"
    case Tab.Datatable => "#datatable"
  }
}

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
    val form       = FormFromJsonSchema.convert(MyForm.jsonSchema)
    val initialTab = Tab.fromHash(dom.window.location.hash).getOrElse(Tab.Forms)
    (
      Model(
        formState = FormElementState.empty(form),
        formsPage = Froms4sPlayground.empty(),
        datatablePage = DatatablePlayground.empty(),
        activeTab = initialTab,
      ),
      Cmd.None,
    )
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.SwitchTab(tab) =>
      (model.copy(activeTab = tab), Nav.pushUrl(Tab.toHash(tab)))

    case Msg.SetTabFromUrl(tab) =>
      (model.copy(activeTab = tab), Cmd.None)

    case Msg.DatatableMsg(dtMsg) =>
      val (newPage, cmd) = model.datatablePage.update(dtMsg)
      (model.copy(datatablePage = newPage), cmd.map(Msg.DatatableMsg.apply))

    case formsMsg =>
      val (newPage, cmd) = model.formsPage.update(formsMsg)
      (model.copy(formsPage = newPage), cmd)
  }

  def encodeFormToQueryParam(state: FormElementState): String =
    URLEncoder.encode(state.extractJson.noSpaces, "UTF-8")

  def view(model: Model): Html[Msg] =
    div(className := "container")(
      h1(className := "title")("Forms4s Playground"),
      renderTabs(model.activeTab),
      hr(),
      model.activeTab match {
        case Tab.Forms     => model.formsPage.render
        case Tab.Datatable => model.datatablePage.render.map(Msg.DatatableMsg.apply)
      },
    )

  def renderTabs(activeTab: Tab): Html[Msg] =
    div(className := "tabs is-boxed")(
      Html.ul(
        Html.li(if (activeTab == Tab.Forms) className := "is-active" else className := "")(
          a(onClick(Msg.SwitchTab(Tab.Forms)))("Forms Demo"),
        ),
        Html.li(if (activeTab == Tab.Datatable) className := "is-active" else className := "")(
          a(onClick(Msg.SwitchTab(Tab.Datatable)))("Datatable Demo"),
        ),
      ),
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.Batch(urlParamsSub, hashChangeSub)

  def urlParamsSub: Sub[IO, Msg] =
    Sub.emit {
      val params = new URLSearchParams(dom.window.location.search)
      val json   = Option(params.get("form"))
      json match {
        case Some(value) => Msg.HydrateFormFromUrl(value)
        case None        => Msg.NoOp
      }
    }

  def hashChangeSub: Sub[IO, Msg] =
    Sub.fromEvent[IO, dom.HashChangeEvent, Msg]("hashchange", dom.window) { _ =>
      Tab.fromHash(dom.window.location.hash).map(Msg.SetTabFromUrl.apply)
    }
}

case class Model(
    formState: FormElementState,
    formsPage: Froms4sPlayground,
    datatablePage: DatatablePlayground,
    activeTab: Tab,
)

enum Msg {
  case FormUpdated(raw: FormElementUpdate)
  case SchemaUpdated(rawSchema: String)
  case JsonUpdated(rawJson: String)
  case Submit
  case NoOp
  case HydrateFormFromUrl(json: String)
  case FrameworkSelected(framework: CssFramework)
  case LoadSchema(schema: ASchema)
  case SwitchTab(tab: Tab)
  case SetTabFromUrl(tab: Tab)
  case DatatableMsg(msg: forms4s.example.components.DatatableMsg)
}
