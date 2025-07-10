package forms4s.validation

import scala.util.matching.Regex

class RegexValidator(format: Regex) extends Validator[String] {
  override def validate(in: String): Option[String] =
    if format.matches(in) then None
    else Some(s"Value does not match format ${format.pattern}")

  override def triggers: Set[Validator.ExecutionTrigger] =
    Set(Validator.ExecutionTrigger.Change)
}
