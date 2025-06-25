package forms4s

import forms4s.FormElement.Validator
import forms4s.FormElement.Validator.ExecutionTrigger

sealed trait FormElementState {
  type Self <: FormElementState
  type Value
  def element: FormElement.WithState[Value]
  def id: String = element.core.id
  def value: Value
  def setErrors(errors: Seq[String]): Self
  protected def updatePF: PartialFunction[FormElementUpdate, Self]

  def update(msg: FormElementUpdate): FormElementState = msg match {
    case change: FormElementUpdate.Change  => updatePF(change).validate(ExecutionTrigger.Change)
    case FormElementUpdate.Debounced       => this.validate(ExecutionTrigger.Debounce)
    case FormElementUpdate.SubmitAttempted => this.validate(ExecutionTrigger.Submit)
    case FormElementUpdate.Unfocused       => this.validate(ExecutionTrigger.Unfocus)
  }

  private def validate(trigger: Validator.ExecutionTrigger): Self = {
    val validators = element.core.validators.filter(_.triggers.contains(trigger))
    val errors     = validators.flatMap(_.validate(this.value))
    setErrors(errors)
  }
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
    case x: FormElement.Text       => Text(x, "", Nil)
    case x: FormElement.Select     => Select(x, x.options.headOption.getOrElse(""), Nil)
    case x: FormElement.Checkbox   => Checkbox(x, false, Nil)
    case x: FormElement.Group      => Group(x, x.elements.map(empty), Nil)
    case x: FormElement.Number     => Number(x, 0.0, Nil)
    case x: FormElement.Multivalue => Multivalue(x, Vector(), Nil)
  }

  case class Text(element: FormElement.Text, value: String, errors: Seq[String])                               extends FormElementState {
    override type Self  = Text
    override type Value = String
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Text(v) => copy(value = v) }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Number(element: FormElement.Number, value: Double, errors: Seq[String])                           extends FormElementState {
    override type Self  = Number
    override type Value = Double
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Number(v) => copy(value = v) }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Select(element: FormElement.Select, value: String, errors: Seq[String])                           extends FormElementState {
    override type Self  = Select
    override type Value = String
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Select(v) => copy(value = v) }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Checkbox(element: FormElement.Checkbox, value: Boolean, errors: Seq[String])                      extends FormElementState {
    override type Self  = Checkbox
    override type Value = Boolean
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Checkbox(v) => copy(value = v) }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Group(element: FormElement.Group, value: List[FormElementState], errors: Seq[String])            extends FormElementState {
    override type Self  = Group
    override type Value = List[FormElementState]
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Nested(field, newValue) =>
      val idx = value.indexWhere(_.id == field)
      copy(value = value.updated(idx, value(idx).update(newValue)))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Multivalue(element: FormElement.Multivalue, value: Vector[FormElementState], errors: Seq[String]) extends FormElementState {
    override type Self = Multivalue
    override type Value         = Vector[FormElementState]
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = {
      case FormElementUpdate.MultivalueUpdate(idx, newValue) => copy(value = value.updated(idx, value(idx).update(newValue)))
      case FormElementUpdate.MultivalueAppend                => copy(value = value.appended(empty(element.item)))
      case FormElementUpdate.MultivalueRemove(idx)           => copy(value = value.patch(idx, Nil, 1))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
}
