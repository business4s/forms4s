package forms4s

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

sealed trait FormElement {
  type State
  def core: FormElement.Core[State]
}

object FormElement {
  type WithState[S] = FormElement { type State = S }

  case class Text(core: Core[String], multiline: Boolean)                           extends FormElement {
    type State = String
  }
  case class Number(core: Core[Option[Double]], isInteger: Boolean)                         extends FormElement {
    type State = Option[Double]
  }
  case class Select(core: Core[String], options: List[String])                      extends FormElement {
    type State = String
  }
  case class Checkbox(core: Core[Boolean])                                          extends FormElement {
    type State = Boolean
  }
  case class Group(core: Core[List[FormElementState]], elements: List[FormElement]) extends FormElement {
    type State = List[FormElementState]
  }
  case class Multivalue(core: Core[Vector[FormElementState]], item: FormElement)    extends FormElement {
    type State = Vector[FormElementState]
  }

  case class Time(core: Core[OffsetTime]) extends FormElement {
    type State = OffsetTime
  }

  case class Date(core: Core[LocalDate]) extends FormElement {
    type State = LocalDate
  }

  case class DateTime(core: Core[OffsetDateTime]) extends FormElement {
    // We could be using ZonedDateTime but browsers dont ship with IANA database
    type State = OffsetDateTime
  }
  case class Alternative(core: Core[Alternative.State], variants: Seq[FormElement], discriminator: Option[String]) extends FormElement {
    override type State = Alternative.State
  }
  object Alternative {
    case class State(selected: Int, states: Vector[FormElementState])
  }

  case class Core[-T](id: String, label: String, description: Option[String], validators: Seq[Validator[T]])

  trait Validator[-T] {
    def validate(in: T): Option[String]
    def triggers: Set[Validator.ExecutionTrigger]
  }

  object Validator {
    enum ExecutionTrigger {
      case Change, Submit, Debounce, Unfocus
    }
  }

}
