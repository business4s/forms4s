package forms4s.tyrian

import forms4s.FormElement.Text.Format
import forms4s.{FormElementState, FormElementUpdate}
import tyrian.*
import tyrian.Html.value

import scala.annotation.unused

/** Abstraction for rendering form elements with Tyrian */
trait FormRenderer {

  def renderForm(state: FormElementState): Html[FormElementUpdate] = state match {
    case text: FormElementState.Text         => renderTextInput(text)
    case select: FormElementState.Select     => renderSelect(select)
    case checkbox: FormElementState.Checkbox => renderCheckbox(checkbox)
    case subform: FormElementState.Group     => renderGroup(subform)
    case number: FormElementState.Number     => renderNumberInput(number)
    case number: FormElementState.Multivalue => renderMultivalue(number)
    case state: FormElementState.Alternative => renderAlternative(state)
  }

  protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]

  protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate]

  protected def renderSelect(state: FormElementState.Select): Html[FormElementUpdate]

  protected def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate]

  protected def renderGroup(state: FormElementState.Group): Html[FormElementUpdate]

  protected def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate]

  protected def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate]

  extension (s: FormElementState) {
    protected def htmlId: Attribute   = Html.id      := s.path.asHtmlId
    protected def htmlName: Attribute = Html.name    := s.path.asHtmlId
    protected def htmlFor: Attribute  = Html.htmlFor := s.path.asHtmlId
    protected def label: String       = s.element.core.label
  }
  extension (s: FormElementState.SimpleInputBased) {
    protected def htmlOnInput: Attr[FormElementUpdate] = Html.onInput(x => {
      println(s"onInput: $x")
      s.emitUpdate(x)
    })
    protected def htmlValue: PropertyString            = Html.value := s.valueToString(s.value)

    protected def htmlType: Attribute = {
      Html.`type` := (s match {
        case x: FormElementState.Text   =>
          x.element.format match {
            case Format.Raw       => "text"
            case Format.Date      => "date"
            case Format.Time      => "time"
            case Format.DateTime  => "datetime-local"
            case Format.Email     => "email"
            case Format.Multiline => "textarea"
            case Format.Custom(_) => "text"
          }
        case _: FormElementState.Number => "number"
      })
    }
  }
  extension (s: FormElementState.Alternative) {
    protected def htmlOnChange: Attr[FormElementUpdate]   = Html.onChange(x => FormElementUpdate.AlternativeSelected(x.toInt))
    protected def htmlOptions: List[Html[Nothing]]        = s.element.variants.toList.zipWithIndex.map { case (elem, idx) =>
      Html.option(value := idx.toString, Html.selected := (idx == s.value.selected))(elem.core.label)
    }
    protected def renderSelected: Html[FormElementUpdate] = renderForm(s.selected).map(update => FormElementUpdate.Nested(s.value.selected, update))
  }
  extension (s: FormElementState.Select) {
    protected def htmlOnChange: Attr[FormElementUpdate] = Html.onChange(s.emitUpdate)
    protected def htmlOptions: List[Html[Nothing]]      = s.element.options.map(option => Html.option(value := option, Html.selected := (s.value == option))(option))
  }
  extension (s: FormElementState.Checkbox) {
    protected def htmlType: Attribute                   = Html.`type`  := "checkbox"
    protected def htmlChecked: PropertyBoolean          = Html.checked := s.value
    protected def htmlOnChange: Attr[FormElementUpdate] =
      Html.onEvent("change", (e: Tyrian.Event) => s.emitUpdate(e.target.asInstanceOf[Tyrian.HTMLInputElement].checked))
  }
  extension (s: FormElementState.Group) {
    protected def renderElements: List[Html[FormElementUpdate.Nested]] =
      s.value.zipWithIndex.map((subElement, idx) => renderForm(subElement).map(x => FormElementUpdate.Nested(idx, x)))
  }
  extension (@unused s: FormElementState.Multivalue) {
    protected def htmlOnClickAdd = Html.onClick(FormElementUpdate.MultivalueAppend)
  }
  case class MultivalueElement(elem: FormElementState, index: Int) {
    def render: Html[FormElementUpdate.MultivalueUpdate] = renderForm(elem).map(x => FormElementUpdate.MultivalueUpdate(index, x))
    def htmlOnClickRemove                                = Html.onClick(FormElementUpdate.MultivalueRemove(index))
  }
  extension (s: FormElementState.Multivalue) {
    protected def elements: List[MultivalueElement] = s.value.zipWithIndex.map(MultivalueElement.apply).toList
  }
  extension (s: FormElementState.Number) {
    protected def htmlStep: Attr[FormElementUpdate] = Html.step := (if (s.element.isInteger) then "1" else "any")
  }
}

object FormRenderer {

  case class Path(segments: List[String]) {
    def /(segment: String): Path = Path(segment :: segments)
    def asString: String         = segments.mkString(".")
  }

}
