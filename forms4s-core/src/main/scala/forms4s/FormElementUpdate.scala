package forms4s

sealed trait FormElementUpdate

object FormElementUpdate {
  sealed trait Change                                     extends FormElementUpdate

  case class ValueUpdate[T](value: T)                     extends Change
  case class Nested(field: Int, value: FormElementUpdate) extends Change

  case class AlternativeSelected(index: Int) extends Change

  case class MultivalueUpdate(index: Int, value: FormElementUpdate) extends Change
  case object MultivalueAppend                                      extends Change
  case class MultivalueRemove(index: Int)                           extends Change

  case class Debounced(fields: List[String]) extends FormElementUpdate
  case object SubmitAttempted                extends FormElementUpdate
  case class Unfocused(fields: List[String]) extends FormElementUpdate
}
