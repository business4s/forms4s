package forms4s.example

import cats.effect.IO
import forms4s.FormState
import forms4s.circe.FormStateEncoder.extractJson
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.tyrian.*
import tyrian.*
import tyrian.Html.*

import scala.scalajs.js.annotation.*

object MyForm {

  import sttp.apispec.Schema as ASchema
  import sttp.tapir.Schema as TSchema
  import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema

  case class Address(
      street: String,
      city: String,
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

  def router: Location => Msg =
    Routing.none(Msg.NoOp)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val form = FormFromJsonSchema.convert(MyForm.jsonSchema)
    (Model(FormState.empty(form)), Cmd.None)
  }

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.UpdateMyForm(raw: FormUpdate) =>
      val newState = model.formState.update(raw.field, raw.value)
      (model.copy(formState = newState), Cmd.None)
    case Msg.Submit                        =>
      println(s"Form submitted with data: ${model.formState.extractJson}")
      (model, Cmd.None)
    case Msg.NoOp                          =>
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
