package forms4s


import forms4s.validation.Validator
import forms4s.validation.Validator.ExecutionTrigger

import scala.reflect.ClassTag

sealed trait FormElementState {
  type Self <: FormElementState
  val element: FormElement
  def id: String                 = element.core.id
  def value: element.State
  def parentPath: FormElementPath
  lazy val path: FormElementPath = parentPath / element.core.id

  def errors: Seq[String]
  def setErrors(errors: Seq[String]): Self

  protected def updatePF: PartialFunction[FormElementUpdate, Self]

  def update(msg: FormElementUpdate): FormElementState = msg match {
    case change: FormElementUpdate.Change  => updatePF(change).validate(ExecutionTrigger.Change)
    // TODO this is wrong, doesnt handle recursion (going inside) of groups/multivalues
    case FormElementUpdate.SubmitAttempted => this.validate(ExecutionTrigger.Submit)
    case FormElementUpdate.Debounced(_)    => this.validate(ExecutionTrigger.Debounce)
    case FormElementUpdate.Unfocused(_)    => this.validate(ExecutionTrigger.Unfocus)
  }

  private def validate(trigger: Validator.ExecutionTrigger): Self = {
    val validators = element.core.validators.filter(_.triggers.contains(trigger))
    val errors     = validators.flatMap(_.validate(this.value))
    setErrors(errors)
  }

}

object FormElementState {

  type ForElem[T <: FormElement] = T match {
    case FormElement.Text        => Text
    case FormElement.Select      => Select
    case FormElement.Checkbox    => Checkbox
    case FormElement.Group       => Group
    case FormElement.Number      => Number
    case FormElement.Multivalue  => Multivalue
    case FormElement.Alternative => Alternative
  }

  def empty[T <: FormElement](elem: T): ForElem[T] = {
    def go[T <: FormElement](elem: T, parentPath: FormElementPath): ForElem[T] = elem match {
      case x: FormElement.Text        => Text(x, "", Nil, parentPath)
      case x: FormElement.Select      => Select(x, x.options.headOption.getOrElse(""), Nil, parentPath)
      case x: FormElement.Checkbox    => Checkbox(x, false, Nil, parentPath)
      case x: FormElement.Group       => Group(x, x.elements.map(go(_, parentPath / x.core.id)), Nil, parentPath)
      case x: FormElement.Number      => Number(x, None, Nil, parentPath)
      case x: FormElement.Multivalue  => Multivalue(x, Vector(), Nil, parentPath)
      case x: FormElement.Alternative =>
        Alternative(x, FormElement.Alternative.State(0, x.variants.toVector.map(go(_, parentPath / x.core.id))), Seq(), parentPath)
    }
    go(elem, FormElementPath.Root)
  }

  sealed trait SimpleInputBased extends FormElementState {
    def valueToString(value: element.State): String
    def valueFromString(value: String): element.State
    def emitUpdate(newValue: String): FormElementUpdate = FormElementUpdate.ValueUpdate(valueFromString(newValue))
  }

  case class Text(element: FormElement.Text, value: String, errors: Seq[String], parentPath: FormElementPath)             extends SimpleInputBased {
    override type Self = Text
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value
    def valueFromString(value: String): element.State                         = value
  }
  case class Number(element: FormElement.Number, value: Option[Double], errors: Seq[String], parentPath: FormElementPath) extends SimpleInputBased {
    override type Self = Number
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.map(_.toString).getOrElse("")
    def valueFromString(value: String): element.State                         = if (value.isBlank) None else (Some(value.toDouble))
  }
  case class Select(element: FormElement.Select, value: String, errors: Seq[String], parentPath: FormElementPath)         extends FormElementState {
    override type Self = Select
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def emitUpdate(newValue: String): FormElementUpdate                       = FormElementUpdate.ValueUpdate(newValue)
  }
  case class Checkbox(element: FormElement.Checkbox, value: Boolean, errors: Seq[String], parentPath: FormElementPath)    extends FormElementState {
    override type Self = Checkbox
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def emitUpdate(newValue: Boolean): FormElementUpdate                      = FormElementUpdate.ValueUpdate(newValue)
  }
  case class Group(element: FormElement.Group, value: List[FormElementState], errors: Seq[String], parentPath: FormElementPath)
      extends FormElementState {
    override type Self = Group
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Nested(idx, newValue) =>
      copy(value = value.updated(idx, value(idx).update(newValue)))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Multivalue(element: FormElement.Multivalue, value: Vector[FormElementState], errors: Seq[String], parentPath: FormElementPath)
      extends FormElementState {
    override type Self = Multivalue
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = {
      case FormElementUpdate.MultivalueUpdate(idx, newValue) => copy(value = value.updated(idx, value(idx).update(newValue)))
      case FormElementUpdate.MultivalueAppend                => copy(value = value.appended(empty(element.item)))
      case FormElementUpdate.MultivalueRemove(idx)           => copy(value = value.patch(idx, Nil, 1))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }

  case class Alternative(element: FormElement.Alternative, value: FormElement.Alternative.State, errors: Seq[String], parentPath: FormElementPath)
      extends FormElementState {
    override type Self = Alternative
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = {
      case FormElementUpdate.AlternativeSelected(idx) => copy(value = value.copy(selected = idx))
      case FormElementUpdate.Nested(idx, update)      => copy(value = value.copy(states = value.states.updated(idx, value.states(idx).update(update))))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)

    def selected: FormElementState = value.states(value.selected)
  }

  private def valueUpdate[T: {ClassTag as ct}, Self](f: T => Self): PartialFunction[FormElementUpdate, Self] = {
    case FormElementUpdate.ValueUpdate(ct(value)) => f(value)
  }

}
