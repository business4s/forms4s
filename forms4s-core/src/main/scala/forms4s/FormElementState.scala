package forms4s

sealed trait FormElementState {
  type Self <: FormElementState
  def element: FormElement
  def id: String                           = element.core.id
  def update(msg: FormElementUpdate): Self = this.updatePF(msg)

  protected def updatePF: PartialFunction[FormElementUpdate, Self]
}

object FormElementState {

  type ForElem[T <: FormElement] = T match {
    case FormElement.Text       => Text
    case FormElement.Select     => Select
    case FormElement.Checkbox   => Checkbox
    case FormElement.Group      => Group
    case FormElement.Number     => Number
    case FormElement.Multivalue => Multivalue
  }

  def empty[T <: FormElement](elem: T): ForElem[T] = elem match {
    case x: FormElement.Text       => Text(x, "")
    case x: FormElement.Select     => Select(x, x.options.headOption.getOrElse(""))
    case x: FormElement.Checkbox   => Checkbox(x, false)
    case x: FormElement.Group      => Group(x, x.elements.map(empty))
    case x: FormElement.Number     => Number(x, 0.0)
    case x: FormElement.Multivalue => Multivalue(x, Vector())
  }

  case class Text(element: FormElement.Text, value: String)                                extends FormElementState {
    override type Self = Text
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Text(v) => copy(value = v) }
  }
  case class Number(element: FormElement.Number, value: Double)                            extends FormElementState {
    override type Self = Number
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Number(v) => copy(value = v) }
  }
  case class Select(element: FormElement.Select, value: String)                            extends FormElementState {
    override type Self = Select
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Select(v) => copy(value = v) }
  }
  case class Checkbox(element: FormElement.Checkbox, value: Boolean)                       extends FormElementState {
    override type Self = Checkbox
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Checkbox(v) => copy(value = v) }
  }
  case class Group(element: FormElement.Group, values: List[FormElementState])             extends FormElementState {
    override type Self = Group
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Nested(field, value) =>
      val idx = values.indexWhere(_.id == field)
      copy(values = values.updated(idx, values(idx).update(value)))
    }
  }
  case class Multivalue(element: FormElement.Multivalue, values: Vector[FormElementState]) extends FormElementState {
    override type Self = Multivalue
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = {
      case FormElementUpdate.MultivalueUpdate(idx, value) => copy(values = values.updated(idx, values(idx).update(value)))
      case FormElementUpdate.MultivalueAppend()           => copy(values = values.appended(empty(element.item)))
      case FormElementUpdate.MultivalueRemove(idx)        => copy(values = values.patch(idx, Nil, 1))
    }
  }
}
