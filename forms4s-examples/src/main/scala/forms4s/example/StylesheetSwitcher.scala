package forms4s.example

import forms4s.FormStylesheet
import forms4s.tyrian.{FormRenderer, DefaultFormRenderer, BootstrapFormRenderer}
import tyrian.*
import tyrian.Html.*
import org.scalajs.dom.html

/** Represents a CSS framework that can be used for styling forms.
  */
enum CssFramework:
  case Pico, Bulma, Bootstrap

/** Provides functionality for switching between different CSS frameworks.
  */
object StylesheetSwitcher:

  /** Gets the stylesheet for the specified CSS framework.
    */
  def getStylesheet(framework: CssFramework): FormStylesheet = framework match
    case CssFramework.Pico => PicoStylesheet.stylesheet
    case CssFramework.Bulma => BulmaStylesheet.stylesheet
    case CssFramework.Bootstrap => BootstrapStylesheet.stylesheet

  /** Gets the form renderer for the specified CSS framework.
    */
  def getRenderer(framework: CssFramework): FormRenderer = framework match
    case CssFramework.Bootstrap => BootstrapFormRenderer
    case _ => DefaultFormRenderer

  /** Gets the CSS import path for the specified CSS framework.
    */
  def getCssPath(framework: CssFramework): String = framework match
    case CssFramework.Pico => "@picocss/pico/css/pico.min.css"
    case CssFramework.Bulma => "bulma/css/bulma.min.css"
    case CssFramework.Bootstrap => "bootstrap/dist/css/bootstrap.min.css"

  /** Renders a switcher UI component for selecting a CSS framework.
    */
  def renderSwitcher(
      currentFramework: CssFramework,
      onSelect: CssFramework => Msg
  ): Html[Msg] =
    div(className := "framework-switcher")(
      label(className := "framework-switcher-label")("CSS Framework: "),
      select(
        className := "framework-switcher-select",
        onEvent(
          "change",
          (e) => {
            val value = e.target.asInstanceOf[html.Select].value
            value match
              case "pico" => onSelect(CssFramework.Pico)
              case "bulma" => onSelect(CssFramework.Bulma)
              case "bootstrap" => onSelect(CssFramework.Bootstrap)
              case _ => onSelect(CssFramework.Pico)
          }
        )
      )(
        option(
          value := "pico",
          if currentFramework == CssFramework.Pico then selected := true else EmptyAttribute
        )("Pico CSS"),
        option(
          value := "bulma",
          if currentFramework == CssFramework.Bulma then selected := true else EmptyAttribute
        )("Bulma"),
        option(
          value := "bootstrap",
          if currentFramework == CssFramework.Bootstrap then selected := true else EmptyAttribute
        )("Bootstrap")
      )
    )

  /** Renders an iframe containing the form with the specified CSS framework.
    */
  def renderFormInIframe(
      framework: CssFramework,
      content: Html[Msg]
  ): Html[Msg] =
    iframe(
      id := s"form-iframe-${framework.toString.toLowerCase}",
      style := "width: 100%; height: 1200px; border: none;",
      srcdoc := generateIframeContent(framework, content)
    )()

  /** Generates HTML content for an iframe with the specified CSS framework.
    */
  private def generateIframeContent(
      framework: CssFramework,
      content: Html[Msg]
  ): String =
    s"""
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>Form with ${framework.toString}</title>
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/${getCssPath(framework)}">
    </head>
    <body>
      <div class="container" style="padding: 20px;">
        ${content.render}
      </div>
    </body>
    </html>
    """
