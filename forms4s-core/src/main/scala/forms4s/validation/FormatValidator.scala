package forms4s.validation

class FormatValidator(format: String, validation: String => Boolean, example: Option[String]) extends Validator[String] {

  private lazy val exampleStr = example.map(ex => s", for example: $ex").getOrElse("")

  override def validate(in: String): Option[String] = Option.when(!validation(in))(s"Value should follow ${format} format${exampleStr}")

  override def triggers: Set[Validator.ExecutionTrigger] = Set(Validator.ExecutionTrigger.Change)
}
