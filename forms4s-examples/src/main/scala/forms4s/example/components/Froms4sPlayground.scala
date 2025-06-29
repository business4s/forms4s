package forms4s.example.components

import forms4s.FormElementState
import forms4s.example.{BulmaStylesheet, Model, Msg, MyForm}
import forms4s.tyrian.{BulmaFormRenderer, FormRenderer}
import sttp.apispec.Schema as ASchema
import cats.effect.IO
import forms4s.circe.{FormStateFromJson, FormStateToJson}
import forms4s.circe.FormStateToJson.extractJson
import forms4s.example.components.Froms4sPlayground
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.tyrian.*
import forms4s.{FormElementState, FormElementUpdate}
import org.scalajs.dom
import org.scalajs.dom.{URLSearchParams, document}
import sttp.tapir.Schema.annotations.validate
import sttp.tapir.Validator.Pattern
import tyrian.*
import tyrian.Html.*
import io.circe.Json
import forms4s.circe.FormStateToJson.extractJson

class CodeView()                            {
  def render: Html[Msg] = div()
}
class SchemaView(schema: ASchema)           {
  def render: Html[Msg] = {
    import io.circe.syntax.*
    import sttp.apispec.openapi.circe.*
    div(
      textarea(
        value          := schema.asJson.spaces2,
        rows           := 20,
        Html.className := "textarea ",
        onChange(value => Msg.SchemaUpdated(value))
      )(),
    )
  }
}
case class FormView(form: FormElementState) {
  private val renderer: FormRenderer = BulmaFormRenderer
  def render: Html[Msg]              =
    div()(
      renderer.renderElement(form).map(Msg.FormUpdated.apply),
      div(className := "form-actions")(
        button(
          onClick(Msg.Submit),
          className := "button is-primary",
        )("Submit"),
      ),
    )

}
class JsonView(json: Json) {
  def render: Html[Msg] = {
    div(
      textarea(
        value          := json.spaces2,
        rows           := 20,
        Html.className := "textarea",
        onChange(value => Msg.JsonUpdated(value)),
      )(),
    )
  }
}

case class Froms4sPlayground(
    codeView: CodeView,
    schemaView: SchemaView,
    formView: FormView,
    jsonView: JsonView,
) {

  def update: Msg => (Froms4sPlayground, Cmd[IO, Msg]) = {
    case Msg.FormUpdated(raw)         =>
      val newState = formView.form.update(raw)
      val newJson  = FormStateToJson.extract(newState)
      copy(formView = FormView(newState), jsonView = JsonView(newJson)) -> Cmd.None
    case Msg.SchemaUpdated(rawSchema) =>
      import sttp.apispec.openapi.circe.*
      io.circe.parser.decode[ASchema](rawSchema) match {
        case Left(value)  =>
          // TODO error logging
          (this, Cmd.None)
        case Right(value) =>
          val newForm  = FormFromJsonSchema.convert(value)
          val newState = FormElementState.empty(newForm)
          val newJson  = FormStateToJson.extract(newState)
          copy(schemaView = SchemaView(value), formView = FormView(newState), jsonView = JsonView(newJson)) -> Cmd.None

      }
    case Msg.JsonUpdated(rawJson)     =>
      (for {
        parsed  <- io.circe.parser.parse(rawJson).toOption // TODO error logging
        updates  = FormStateFromJson.hydrate(formView.form, parsed)
        newState = updates.foldLeft(formView.form)((acc, update) => acc.update(update))
      } yield this.copy(jsonView = JsonView(parsed), formView = FormView(newState)) -> Cmd.None).getOrElse((this, Cmd.None))
    case Msg.Submit                   => (this, Cmd.None)
    case Msg.NoOp                     => (this, Cmd.None)
    case Msg.HydrateFormFromUrl(json) => (this, Cmd.None)
  }

  def render: Html[Msg] =
    div(className := "container")(
      div(className := "columns is-multiline")(
        div(className := "column is-half")(
          section(className := "box")(
            h2(className := "title is-5")("Scala Source Code"),
            div(id := "scala-code")(codeView.render),
          ),
        ),
        div(className := "column is-half")(
          section(className := "box")(
            h2(className := "title is-5")("JSON Schema"),
            div(id := "json-schema")(schemaView.render),
          ),
        ),
        div(className := "column is-half")(
          section(className := "box")(
            h2(className := "title is-5")("Generated Form"),
            div(id := "form-container")(formView.render),
          ),
        ),
        div(className := "column is-half")(
          section(className := "box")(
            h2(className := "title is-5")("Extracted JSON"),
            div(id := "json-output")(jsonView.render),
          ),
        ),
      ),
    )
}

object Froms4sPlayground {

  def empty(): Froms4sPlayground = {
    val schema    = MyForm.jsonSchema
    val form      = FormFromJsonSchema.convert(schema)
    val formState = FormElementState.empty(form)
    val json      = FormStateToJson.extract(formState)
    Froms4sPlayground(
      CodeView(),
      SchemaView(schema),
      FormView(formState),
      JsonView(json),
    )
  }
}
