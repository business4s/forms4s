package forms4s

sealed trait FormElement {
  type State
  def core: FormElement.Core[State]
  def stateUpdater: PartialFunction[FormElementUpdate, State => State]
}

object FormElement {
  type WithState[S] = FormElement { type State = S }

  case class Text(core: Core[String], multiline: Boolean)                              extends FormElement {
    type State = String

    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = { case FormElementUpdate.Text(value) =>
      _ => value
    }
  }
  case class Number(core: Core[Double])                                                extends FormElement {
    type State = Double

    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = { case FormElementUpdate.Number(value) =>
      _ => value
    }
  }
  case class Select(core: Core[String], options: List[String])                         extends FormElement {
    type State = String

    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = { case FormElementUpdate.Select(value) =>
      _ => value
    }
  }
  case class Checkbox(core: Core[Boolean])                                             extends FormElement {
    type State = Boolean

    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = { case FormElementUpdate.Checkbox(value) =>
      _ => value
    }
  }
  case class Group(core: Core[List[FormElementState]], elements: List[FormElement]) extends FormElement {
    type State = List[FormElementState]

    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = { case FormElementUpdate.Nested(field, value) =>
      values =>
        val idx = values.indexWhere(_.id == field)
        values.updated(idx, values(idx).update(value))
    }
  }
  case class Multivalue(core: Core[Vector[FormElementState]], item: FormElement)    extends FormElement {
    type State = Vector[FormElementState]
    override def stateUpdater: PartialFunction[FormElementUpdate, State => State] = {
      case FormElementUpdate.MultivalueUpdate(idx, value) => values => values.updated(idx, values(idx).update(value))
      case FormElementUpdate.MultivalueAppend             => _.appended(FormElementState.empty(item))
      case FormElementUpdate.MultivalueRemove(idx)        => _.patch(idx, Nil, 1)
    }
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
