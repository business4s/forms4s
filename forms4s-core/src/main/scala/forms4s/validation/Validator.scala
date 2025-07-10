package forms4s.validation

trait Validator[-T] {
  def validate(in: T): Option[String]
  def triggers: Set[Validator.ExecutionTrigger]
}

object Validator {
  enum ExecutionTrigger {
    case Change, Submit, Debounce, Unfocus
  }
}
