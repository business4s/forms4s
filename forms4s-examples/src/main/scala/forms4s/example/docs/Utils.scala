package forms4s.example.docs

import forms4s.FormElement
import forms4s.FormElement.{Alternative, Checkbox, Group, Multivalue, Number, Select, Text}

object Utils {
  def describeFormElement(fe: FormElement): String = fe match {
    case Text(_, fmt) =>
      fmt match {
        case Text.Format.Raw          => "Text"
        case Text.Format.Multiline    => "Text (Multiline)"
        case Text.Format.Date         => "Text (Date)"
        case Text.Format.Time         => "Text (Time)"
        case Text.Format.DateTime     => "Text (DateTime)"
        case Text.Format.Email        => "Text (Email)"
        case Text.Format.Custom(name) => s"Text ($name)"
      }

    case Number(_, true)  => "Number (Integer)"
    case Number(_, false) => "Number (Decimal)"
    case Select(_, _)     => "Select"
    case Checkbox(_)      => "Checkbox"

    case Group(_, elements) =>
      val items = elements.map(e => s"<li>${e.core.id}</li>").mkString
      s"""Group
         |<ul>
         |  $items
         |</ul>""".stripMargin

    case Multivalue(_, _) =>
      "Multivalue"

    case Alternative(_, variants, _) =>
      val variantsHtml = variants.map {
        case g: FormElement.Group =>
          val subitems = g.elements.map(e => s"<li>${e.core.id}</li>").mkString
          s"""
             |<li>
             |  ${g.core.id}
             |  <ul>
             |    $subitems
             |  </ul>
             |</li>
           """.stripMargin
        case other                =>
          s"<li>${other.core.id}</li>"
      }.mkString

      s"""Alternative
         |<ul>
         |  $variantsHtml
         |</ul>""".stripMargin
  }

  def generateHtmlTable(headers: List[String], rows: List[List[String]]): String = {
    val headerRow = headers
      .map(header => s"<th>$header</th>")
      .mkString("\n")

    val tableRows = rows
      .map { row =>
        val cells = row
          .map(cell => s"<td>$cell</td>")
          .mkString("\n")
        s"<tr>\n$cells\n</tr>"
      }
      .mkString("\n")

    s"""<table>
       |<thead>
       |<tr>
       |$headerRow
       |</tr>
       |</thead>
       |<tbody>
       |$tableRows
       |</tbody>
       |</table>""".stripMargin
  }
}
