package forms4s

import forms4s.FormElement.Validator
import forms4s.FormElement.Validator.ExecutionTrigger

import java.time.*
import scala.reflect.ClassTag

sealed trait FormElementState {
  type Self <: FormElementState
  val element: FormElement
  def id: String = element.core.id
  def value: element.State

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
    case FormElement.Text       => Text
    case FormElement.Select     => Select
    case FormElement.Checkbox   => Checkbox
    case FormElement.Group      => Group
    case FormElement.Number     => Number
    case FormElement.Multivalue => Multivalue
    case FormElement.Time       => Time
    case FormElement.Date       => Date
    case FormElement.DateTime   => DateTime
  }

  def empty[T <: FormElement](elem: T): ForElem[T] = elem match {
    case x: FormElement.Text       => Text(x, "", Nil)
    case x: FormElement.Select     => Select(x, x.options.headOption.getOrElse(""), Nil)
    case x: FormElement.Checkbox   => Checkbox(x, false, Nil)
    case x: FormElement.Group      => Group(x, x.elements.map(empty), Nil)
    case x: FormElement.Number     => Number(x, None, Nil)
    case x: FormElement.Multivalue => Multivalue(x, Vector(), Nil)
    case x: FormElement.Time       => Time(x, OffsetTime.ofInstant(Instant.now(), localTZOffset), Nil)
    case x: FormElement.Date       => Date(x, LocalDate.from(ZonedDateTime.ofInstant(Instant.now(), localTZOffset)), Nil)
    case x: FormElement.DateTime   => DateTime(x, OffsetDateTime.from(ZonedDateTime.ofInstant(Instant.now(), localTZOffset)), Nil)
  }

  sealed trait TextBased extends FormElementState {
    def valueToString(value: element.State): String
    def valueFromString(value: String): element.State
    def emitUpdate(newValue: String): FormElementUpdate = FormElementUpdate.ValueUpdate(valueFromString(newValue))
  }

  case class Text(element: FormElement.Text, value: String, errors: Seq[String])                               extends TextBased        {
    override type Self = Text
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value
    def valueFromString(value: String): element.State                         = value
  }
  case class Number(element: FormElement.Number, value: Option[Double], errors: Seq[String])                           extends TextBased        {
    override type Self = Number
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.map(_.toString).getOrElse("")
    def valueFromString(value: String): element.State                         = if (value.isBlank) None else (Some(value.toDouble))
  }
  case class Select(element: FormElement.Select, value: String, errors: Seq[String])                           extends TextBased        {
    override type Self = Select
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value
    def valueFromString(value: String): element.State                         = value
  }
  case class Checkbox(element: FormElement.Checkbox, value: Boolean, errors: Seq[String])                      extends TextBased        {
    override type Self = Checkbox
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.toString
    def valueFromString(value: String): element.State                         = value == "true"
  }
  case class Group(element: FormElement.Group, value: List[FormElementState], errors: Seq[String])             extends FormElementState {
    override type Self = Group
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = { case FormElementUpdate.Nested(field, newValue) =>
      val idx = value.indexWhere(_.id == field)
      copy(value = value.updated(idx, value(idx).update(newValue)))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }
  case class Multivalue(element: FormElement.Multivalue, value: Vector[FormElementState], errors: Seq[String]) extends FormElementState {
    override type Self = Multivalue
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = {
      case FormElementUpdate.MultivalueUpdate(idx, newValue) => copy(value = value.updated(idx, value(idx).update(newValue)))
      case FormElementUpdate.MultivalueAppend                => copy(value = value.appended(empty(element.item)))
      case FormElementUpdate.MultivalueRemove(idx)           => copy(value = value.patch(idx, Nil, 1))
    }
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
  }

  case class Time(element: FormElement.Time, value: OffsetTime, errors: Seq[String]) extends TextBased {
    override type Self = Time
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.toLocalTime.toString
    def valueFromString(value: String): element.State                         = OffsetTime.of(LocalTime.parse(value), this.value.getOffset)
  }

  case class Date(element: FormElement.Date, value: LocalDate, errors: Seq[String]) extends TextBased {
    override type Self = Date
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.toString
    def valueFromString(value: String): element.State                         = LocalDate.parse(value)
  }

  case class DateTime(element: FormElement.DateTime, value: OffsetDateTime, errors: Seq[String]) extends TextBased {
    override type Self = DateTime
    override protected def updatePF: PartialFunction[FormElementUpdate, Self] = valueUpdate[element.State, Self](v => copy(value = v))
    override def setErrors(errors: Seq[String]): Self                         = this.copy(errors = errors)
    def valueToString(value: element.State): String                           = value.toLocalDateTime.toString
    def valueFromString(value: String): element.State                         = OffsetDateTime.of(LocalDateTime.parse(value), this.value.getOffset)
  }

  private def valueUpdate[T: {ClassTag as ct}, Self](f: T => Self): PartialFunction[FormElementUpdate, Self] = {
    case FormElementUpdate.ValueUpdate(ct(value)) => f(value)
  }

  private def localTZOffset = {
    val offsetMinutes = new scala.scalajs.js.Date().getTimezoneOffset()
    val totalSeconds = (-offsetMinutes * 60).toInt
    val zoneOffset = ZoneOffset.ofTotalSeconds(totalSeconds)
    zoneOffset
  }
}
