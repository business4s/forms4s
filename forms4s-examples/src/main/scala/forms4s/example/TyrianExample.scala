package forms4s.example

import cats.effect.IO
import forms4s.circe.FormStateEncoder.extractJson
import forms4s.tyrian.*
import forms4s.{FormFromJsonSchema, FormState}
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.*

object ExampleServer {

  import forms4s.ExampleModel.{Address, User}
  import sttp.apispec.Schema as ASchema
  import sttp.tapir.Schema as TSchema
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  given TSchema[Address] = TSchema.derived

  given userSchema: TSchema[User] = TSchema.derived

  val jsonSchema: ASchema = TapirSchemaToJsonSchema(
    userSchema,
    markOptionsAsNullable = true,
  )

}

@JSExportTopLevel("TyrianApp")
object TyrianExample extends TyrianIOApp[Msg, Model] {

  def router: Location => Msg =
    Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val form = FormFromJsonSchema.convert(ExampleServer.jsonSchema)
    (Model(FormState.empty(form)), Cmd.None)
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.UpdateMyForm(raw: FormUpdate) =>
      val newState = model.formState.update(raw.field, raw.value)
      (model.copy(formState = newState), Cmd.None)
    case Msg.Submit                       =>
      println(s"Form submitted with data: ${model.formState.extractJson}")
      (model, Cmd.None)
    case Msg.NoOp                         =>
      (model, Cmd.None)
  }

  def view(model: Model): Html[Msg] =
    div(className := "container")(
      div(className := "container")(
        h1("Forms4s Tyrian Example"),
        div()(
          model.formState.render(BulmaStylesheet.stylesheet).map(Msg.UpdateMyForm.apply),
          div(className := "form-actions")(
            button(
              onClick(Msg.Submit),
              className := "button is-primary",
            )("Submit"),
          ),
        ),
      ),
    )

  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
}

case class Model(formState: FormState)

enum Msg {
  case UpdateMyForm(raw: FormUpdate)
  case Submit
  case NoOp
}
