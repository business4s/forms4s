package forms4s.example.components

import cats.effect.IO
import forms4s.FormElementState
import forms4s.circe.{FormStateFromJson, FormStateToJson}
import forms4s.example.Msg
import forms4s.jsonschema.FormFromJsonSchema
import forms4s.tyrian.*
import io.circe.Json
import sttp.apispec.Schema as ASchema
import tyrian.*
import tyrian.Html.*
import forms4s.example.MyForm
import forms4s.example.docs.JsonSchemaDocGen

class SchemaView(schema: ASchema) {
  def render: Html[Msg] = {
    import io.circe.syntax.*
    import sttp.apispec.openapi.circe.*
    div(
      div(className := "buttons")(
        button(
          className := "button is-small",
          onClick(Msg.LoadSchema(MyForm.jsonSchema)),
        )("Simple Example"),
        button(
          className := "button is-small",
          onClick(Msg.LoadSchema(JsonSchemaDocGen.allExamplesASSingleSchema)),
        )("Full Demo"),
      ),
      hr(),
      textarea(
        value          := schema.asJson.spaces2,
        rows           := 20,
        Html.className := "textarea ",
        onChange(value => Msg.SchemaUpdated(value)),
      )(),
    )
  }
}

enum CssFramework {
  case Bulma, Bootstrap, Raw, Pico
}

case class FormView(form: FormElementState, framework: CssFramework) {
  private val renderer: FormRenderer = framework match {
    case CssFramework.Bulma     => BulmaFormRenderer
    case CssFramework.Bootstrap => BootstrapFormRenderer
    case CssFramework.Raw       => RawFormRenderer
    case CssFramework.Pico      => RawFormRenderer
  }
  private val rendererLabel: String  = framework match {
    case CssFramework.Bulma     => "bulma"
    case CssFramework.Bootstrap => "bootstrap"
    case CssFramework.Raw       => "raw"
    case CssFramework.Pico      => "picocss"
  }
  def render: Html[Msg]              = {
    div(
      Html.div(`class` := "select is-small")(
        Html.select(
          id   := "css-framework",
          name := "css-framework",
          onChange(value => Msg.FrameworkSelected(CssFramework.valueOf(value))),
        )(
          Html.option(value := CssFramework.Raw.toString, selected := (framework == CssFramework.Raw))("Raw"),
          Html.option(value := CssFramework.Pico.toString, selected := (framework == CssFramework.Pico))("Pico"),
          Html.option(value := CssFramework.Bulma.toString, selected := (framework == CssFramework.Bulma))("Bulma"),
          Html.option(value := CssFramework.Bootstrap.toString, selected := (framework == CssFramework.Bootstrap))("Bootstrap"),
        ),
      ),
      hr(),
      tyrian.Tag(
        "css-separator",
        List(
          Attribute("renderer", rendererLabel),
        ),
        List(
          renderer.renderForm(form).map(Msg.FormUpdated.apply),
          div(className := "form-actions")(
            button(
              onClick(Msg.Submit),
              className := "button is-primary",
            )("Submit"),
          ),
        ),
      ),
    )
  }

}
class JsonView(json: Json) {
  def render: Html[Msg] = {
    div(
      h2(className := "title is-5")("JSON Data"),
      textarea(
        value          := json.spaces2,
        rows           := 20,
        Html.className := "textarea",
        onInput(value => Msg.JsonUpdated(value)),
      )(),
    )
  }
}

case class Froms4sPlayground(
    schemaView: SchemaView,
    formView: FormView,
    jsonView: JsonView,
) {

  def update: Msg => (Froms4sPlayground, Cmd[IO, Msg]) = {
    case Msg.FormUpdated(raw)                =>
      val newState = formView.form.update(raw)
      val newJson  = FormStateToJson.extract(newState)
      copy(formView = formView.copy(form = newState), jsonView = JsonView(newJson)) -> Cmd.None
    case Msg.SchemaUpdated(rawSchema)        =>
      import sttp.apispec.openapi.circe.*
      io.circe.parser.decode[ASchema](rawSchema) match {
        case Left(value)  =>
          // TODO error logging
          (this, Cmd.None)
        case Right(value) =>
          val newForm  = FormFromJsonSchema.convert(value)
          val newState = FormElementState.empty(newForm)
          val newJson  = FormStateToJson.extract(newState)
          copy(schemaView = SchemaView(value), formView = formView.copy(form = newState), jsonView = JsonView(newJson)) -> Cmd.None

      }
    case Msg.JsonUpdated(rawJson)            =>
      (for {
        parsed  <- io.circe.parser.parse(rawJson).toOption // TODO error logging
        updates  = FormStateFromJson.generateUpdates(formView.form, parsed)
        newState = updates.foldLeft(formView.form)((acc, update) => acc.update(update))
      } yield this.copy(jsonView = JsonView(parsed), formView = formView.copy(form = newState)) -> Cmd.None).getOrElse((this, Cmd.None))
    case Msg.Submit                          => (this, Cmd.None)
    case Msg.NoOp                            => (this, Cmd.None)
    case Msg.HydrateFormFromUrl(json)        => (this, Cmd.None)
    case Msg.FrameworkSelected(newFramework) => (this.copy(formView = formView.copy(framework = newFramework)), Cmd.None)
    case Msg.LoadSchema(schema)              =>
      val newForm  = FormFromJsonSchema.convert(schema)
      val newState = FormElementState.empty(newForm)
      val newJson  = FormStateToJson.extract(newState)
      copy(schemaView = SchemaView(schema), formView = formView.copy(form = newState), jsonView = JsonView(newJson)) -> Cmd.None
    // These are handled at the top level, not in this component
    case Msg.SwitchTab(_)   => (this, Cmd.None)
    case Msg.DatatableMsg(_) => (this, Cmd.None)
  }

  def render: Html[Msg] =
    div(className := "container")(
      div(className := "notification is-info is-light")(
        "You can edit all the panes, and changes will be automatically reflected in other places.",
      ),
      div(className := "columns is-multiline")(
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
      SchemaView(schema),
      FormView(formState, CssFramework.Raw),
      JsonView(json),
    )
  }
}
