package forms4s.tyrian

import forms4s.{FormElementState, FormElementUpdate}
import tyrian.Html.value
import tyrian.*

import scala.annotation.unused

/** Abstraction for rendering form elements with Tyrian */
trait FormRenderer {

  def renderElement(state: FormElementState): Html[FormElementUpdate] = state match {
    case text: FormElementState.Text         => renderTextInput(text)
    case select: FormElementState.Select     => renderSelect(select)
    case checkbox: FormElementState.Checkbox => renderCheckbox(checkbox)
    case subform: FormElementState.Group     => renderGroup(subform)
    case number: FormElementState.Number     => renderNumberInput(number)
    case number: FormElementState.Multivalue => renderMultivalue(number)
    case state: FormElementState.Time        => renderTime(state)
    case state: FormElementState.Date        => renderDate(state)
    case state: FormElementState.DateTime    => renderDateTime(state)
    case state: FormElementState.Alternative => renderAlternative(state)
  }

  protected def renderTextInput(state: FormElementState.Text): Html[FormElementUpdate]

  protected def renderNumberInput(state: FormElementState.Number): Html[FormElementUpdate]

  protected def renderSelect(state: FormElementState.Select): Html[FormElementUpdate]

  protected def renderCheckbox(state: FormElementState.Checkbox): Html[FormElementUpdate]

  protected def renderDate(state: FormElementState.Date): Html[FormElementUpdate]
  protected def renderTime(state: FormElementState.Time): Html[FormElementUpdate]
  protected def renderDateTime(state: FormElementState.DateTime): Html[FormElementUpdate]

  protected def renderGroup(state: FormElementState.Group): Html[FormElementUpdate]

  protected def renderMultivalue(state: FormElementState.Multivalue): Html[FormElementUpdate]

  protected def renderAlternative(state: FormElementState.Alternative): Html[FormElementUpdate]

  extension (s: FormElementState) {
    def htmlId: Attribute   = Html.id      := s.path.asHtmlId
    def htmlName: Attribute = Html.name    := s.path.asHtmlId
    def htmlFor: Attribute  = Html.htmlFor := s.path.asHtmlId
    def label: String       = s.element.core.label
  }
  extension (s: FormElementState.SimpleInputBased) {
    def htmlOnInput: Attr[FormElementUpdate] = Html.onInput(x => {
      println(s"onInput: $x")
      s.emitUpdate(x)
    })
    def htmlValue: PropertyString            = Html.value := s.valueToString(s.value)

    def htmlType: Attribute = {
      Html.`type` := (s match {
        case _: FormElementState.Text     => "text"
        case _: FormElementState.Number   => "number"
        case _: FormElementState.Time     => "time"
        case _: FormElementState.Date     => "date"
        case _: FormElementState.DateTime => "datetime-local"
      })
    }
  }
  extension (s: FormElementState.Alternative) {
    def htmlOnChange: Attr[FormElementUpdate]   = Html.onChange(x => FormElementUpdate.AlternativeSelected(x.toInt))
    def htmlOptions: List[Html[Nothing]]        = s.element.variants.toList.zipWithIndex.map { case (elem, idx) =>
      Html.option(value := idx.toString, Html.selected := (idx == s.value.selected))(elem.core.label)
    }
    def renderSelected: Html[FormElementUpdate] = renderElement(s.selected).map(update => FormElementUpdate.Nested(s.value.selected, update))
  }
  extension (s: FormElementState.Select) {
    def htmlOnChange: Attr[FormElementUpdate] = Html.onChange(s.emitUpdate)
    def htmlOptions: List[Html[Nothing]]      = s.element.options.map(option => Html.option(value := option, Html.selected := (s.value == option))(option))
  }
  extension (s: FormElementState.Checkbox) {
    def htmlType: Attribute                   = Html.`type`  := "checkbox"
    def htmlChecked: PropertyBoolean          = Html.checked := s.value
    def htmlOnChange: Attr[FormElementUpdate] =
      Html.onEvent("change", (e: Tyrian.Event) => s.emitUpdate(e.target.asInstanceOf[Tyrian.HTMLInputElement].checked))
  }
  extension (s: FormElementState.Group) {
    def renderElements: List[Html[FormElementUpdate.Nested]] =
      s.value.zipWithIndex.map((subElement, idx) => renderElement(subElement).map(x => FormElementUpdate.Nested(idx, x)))
  }
  extension (@unused s: FormElementState.Multivalue) {
    def htmlOnClickAdd = Html.onClick(FormElementUpdate.MultivalueAppend)
  }
  case class MultivalueElement(elem: FormElementState, index: Int) {
    def render: Html[FormElementUpdate.MultivalueUpdate] = renderElement(elem).map(x => FormElementUpdate.MultivalueUpdate(index, x))
    def htmlOnClickRemove                                = Html.onClick(FormElementUpdate.MultivalueRemove(index))
  }
  extension (s: FormElementState.Multivalue) {
    def elements: List[MultivalueElement] = s.value.zipWithIndex.map(MultivalueElement.apply).toList
  }
  extension (s: FormElementState.Number) {
    def htmlStep: Attr[FormElementUpdate] = Html.step := (if (s.element.isInteger) then "1" else "any")
  }
}

object FormRenderer {

  case class Path(segments: List[String]) {
    def /(segment: String): Path = Path(segment :: segments)
    def asString: String         = segments.mkString(".")
  }

}
