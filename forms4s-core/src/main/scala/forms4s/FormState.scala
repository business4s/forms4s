package forms4s

case class FormState(definition: Form, values: List[FormState.Element]) {

  def update(field: String, value: FormValue): FormState = {
    FormState(
      definition,
      values.map({
        case x if x.name == field => x.update(value)
        case x                    => x
      }),
    )
  }

}

object FormState {

  def empty(form: Form): FormState = {
    FormState(form, form.elements.map(emptyElement))
  }

  def emptyElement(elem: FormElement): Element = elem match {
    case x: FormElement.Text       => Text(x, "")
    case x: FormElement.Select     => Select(x, x.options.headOption.getOrElse(""))
    case x: FormElement.Checkbox   => Checkbox(x, false)
    case x: FormElement.Subform    => Group(x, FormState.empty(x.form))
    case x: FormElement.Number     => Number(x, 0.0)
    case x: FormElement.Multivalue => Multivalue(x, Vector())
  }

  sealed trait Element {
    def element: FormElement
    def name: String                    = element.id
    def update(msg: FormValue): Element = {
      (this, msg) match {
        case (Text(e, _), FormValue.Text(newValue))                              => Text(e, newValue)
        case (Checkbox(e, _), FormValue.Checkbox(newValue))                      => Checkbox(e, newValue)
        case (Select(e, _), FormValue.Select(newValue))                          => Select(e, newValue)
        case (Group(e, fields), FormValue.Nested(field, newValue))               => Group(e, fields.update(field, newValue))
        case (Number(e, _), FormValue.Number(newValue))                          => Number(e, newValue)
        case (Multivalue(e, items), FormValue.MultivalueUpdate(index, newValue)) => Multivalue(e, items.updated(index, items(index).update(newValue)))
        case (Multivalue(e, items), FormValue.MultivalueAppend())                => Multivalue(e, items.appended(emptyElement(e.item)))
        case (Multivalue(e, items), FormValue.MultivalueRemove(index))           => Multivalue(e, items.patch(index, Nil, 1))
        case _                                                                   => ???
      }
    }
  }

  case class Text(element: FormElement.Text, value: String)                                extends Element
  case class Number(element: FormElement.Number, value: Double)                            extends Element
  case class Select(element: FormElement.Select, value: String)                            extends Element
  case class Checkbox(element: FormElement.Checkbox, value: Boolean)                       extends Element
  case class Group(element: FormElement.Subform, value: FormState)                         extends Element
  case class Multivalue(element: FormElement.Multivalue, value: Vector[FormState.Element]) extends Element
}
