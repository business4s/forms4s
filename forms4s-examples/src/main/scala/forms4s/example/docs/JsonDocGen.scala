package forms4s.example.docs

import forms4s.circe.*
import forms4s.{FormElement, FormElementState, FormElementUpdate}
import io.circe.Json

object JsonDocGen {

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

  private def createExample(elem: FormElement): (String, Json, Json) = {
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
        x.update(FormElementUpdate.AlternativeSelected(1))
          .update(FormElementUpdate.Nested(1, FormElementUpdate.Nested(0, FormElementUpdate.ValueUpdate("foo"))))
    }
  }

  def generateDocumentation(): String = {
    val headers = List("FormElement Type", "Empty Value", "Example Value")
    val rows    = examples
      .map(createExample)
      .map { case (desc, emptyJson, exampleJson) =>
        List(
          desc,
          s"""<pre><code class="language-json">${emptyJson.spaces2}</code></pre>""",
          s"""<pre><code class="language-json">${exampleJson.spaces2}</code></pre>""",
        )
      }

    Utils.generateHtmlTable(headers, rows)
  }

}
