package forms4s.example.docs

import forms4s.circe.*
import forms4s.{FormElement, FormElementState, FormElementUpdate}
import io.circe.Json

import java.io.{File, PrintWriter}
import scala.util.Using

class JsonDocGen {

  val examples: List[FormElement] = List(
    FormElement.Text(dummyCore("text"), FormElement.Text.Format.Raw),
    FormElement.Number(dummyCore("number"), isInteger = false),
    FormElement.Select(dummyCore("select"), options = List("Option 1", "Option 2", "Option 3")),
    FormElement.Checkbox(dummyCore("checkbox")),
    FormElement.Group(dummyCore("group"), List(FormElement.Text(dummyCore("field1"), FormElement.Text.Format.Raw))),
    FormElement.Multivalue(dummyCore("multivalue"), FormElement.Text(dummyCore("item"), FormElement.Text.Format.Raw)),
    FormElement.Alternative(
      dummyCore("alternative"),
      variants = List(
        FormElement.Group(dummyCore("alt1"), List(FormElement.Text(dummyCore("field1"), FormElement.Text.Format.Raw))),
        FormElement.Group(dummyCore("alt2"), List(FormElement.Text(dummyCore("field1"), FormElement.Text.Format.Raw))),
      ),
      discriminator = Some("tpe"),
    ),
  )

  private def dummyCore(id: String) = FormElement.Core(id, "", None, Seq())

  def createExample(elem: FormElement): (String, Json, Json) = {
    val empty       = FormElementState.empty(elem)
    val emptyJson   = empty.extractJson
    val updated     = setExampleValue(empty)
    val updatedJson = updated.extractJson
    val description = Utils.describeFormElement(elem)
    (description, emptyJson, updatedJson)
  }

  private def setExampleValue(st: FormElementState): FormElementState = {
    st match {
      case x: FormElementState.Text        => x.update(FormElementUpdate.ValueUpdate("foo"))
      case x: FormElementState.Number      => x.update(x.emitUpdate("1.1"))
      case x: FormElementState.Select      => x.update(FormElementUpdate.ValueUpdate("Option 2"))
      case x: FormElementState.Checkbox    => x.update(FormElementUpdate.ValueUpdate(true))
      case x: FormElementState.Group       => x.update(FormElementUpdate.Nested(0, FormElementUpdate.ValueUpdate("foo")))
      case x: FormElementState.Multivalue  =>
        x.update(FormElementUpdate.MultivalueAppend).update(FormElementUpdate.MultivalueUpdate(0, FormElementUpdate.ValueUpdate("foo")))
      case x: FormElementState.Alternative =>
        x.update(FormElementUpdate.AlternativeSelected(1)).update(FormElementUpdate.Nested(1, FormElementUpdate.Nested(0, FormElementUpdate.ValueUpdate("foo"))))
    }
  }


  /** Renders the full HTML table and writes it out */
  def generateDocumentation(
    outputPath: String = "forms4s-examples/src/test/resources/docs/form-state-examples.md"
  ): Unit = {
    // ensure parent directory exists
    val outFile   = new File(outputPath)
    outFile.getParentFile.mkdirs()

    // build rows
    val rows = examples
      .map(createExample)
      .map { case (desc, emptyJson, exampleJson) =>
        s"""
           |  <tr>
           |    <td>${desc}</td>
           |    <td><pre><code class="language-json">${emptyJson.spaces2}</code></pre></td>
           |    <td><pre><code class="language-json">${exampleJson.spaces2}</code></pre></td>
           |  </tr>
         """.stripMargin
      }.mkString("\n")

    // wrap in a complete HTML table
    val html =
      s"""
         |<table>
         |  <thead>
         |    <tr>
         |      <th>FormElement Type</th>
         |      <th>Empty Value</th>
         |      <th>Example Value</th>
         |    </tr>
         |  </thead>
         |  <tbody>
         |$rows
         |  </tbody>
         |</table>
       """.stripMargin

    // write out
    val _ = Using(new PrintWriter(outFile)) { writer =>
      writer.write(html)
    }

    println(s"Form-state documentation generated at $outputPath")
  }
}

object JsonDocGenApp {
  def main(args: Array[String]): Unit = {
    new JsonDocGen().generateDocumentation()
  }
}
