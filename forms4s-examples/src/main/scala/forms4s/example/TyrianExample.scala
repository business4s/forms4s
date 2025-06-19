package forms4s.example

import cats.effect.IO
import forms4s.ExampleModel.{Address, User}
import forms4s.tyrian.TyrianForm
import forms4s.tyrian.TyrianForm.FormState
import forms4s.{ExampleServer, Form, FormElement, FormFromJsonSchema}
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.*

object ExampleServer {

  import forms4s.ExampleModel.{Address, User}
  import sttp.apispec.{Schema => ASchema}
  import sttp.tapir.{Schema => TSchema}
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  given TSchema[Address] = TSchema.derived

  given userSchema: TSchema[User] = TSchema.derived

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    userSchema,
    markOptionsAsNullable = true,
  )

}

@JSExportTopLevel("TyrianApp")
object TyrianExample extends TyrianIOApp[Msg, Model]:

  def router: Location => Msg =
    Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    val form = FormFromJsonSchema.convert(ExampleServer.jsonSchema)
    (Model(form, FormState()), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Msg.UpdateField(name, value) => 
      val newState = model.formState.update(name, value)
      (model.copy(formState = newState), Cmd.None)
    case Msg.Submit => 
      val data = TyrianForm.extractData(model.form, model.formState)
      println(s"Form submitted with data: $data")
      (model, Cmd.None)
    case Msg.NoOp => 
      (model, Cmd.None)

  def view(model: Model): Html[Msg] =
    div(className := "container")(
      div(className := "container")(
        h1("Forms4s Tyrian Example"),
        TyrianForm.render(
          model.form, 
          model.formState, 
          Msg.UpdateField,
          PicoStylesheet.stylesheet
        ),
        div(className := "grid")(
          button(
            onClick(Msg.Submit),
            className := "submit-button"
          )("Submit")
        )
      )
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None

case class Model(form: Form, formState: FormState)

enum Msg:
  case UpdateField(name: String, value: String)
  case Submit
  case NoOp
