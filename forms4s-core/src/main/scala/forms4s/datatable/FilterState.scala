package forms4s.datatable

import java.time.LocalDate

/** Filter state - the actual runtime value of a filter */
sealed trait FilterState {
  def isEmpty: Boolean
}

object FilterState {
  case class TextValue(value: String) extends FilterState {
    def isEmpty: Boolean = value.isEmpty
  }

  case class SelectValue(selected: Option[String]) extends FilterState {
    def isEmpty: Boolean = selected.isEmpty
  }

  case class MultiSelectValue(selected: Set[String]) extends FilterState {
    def isEmpty: Boolean = selected.isEmpty
  }

  case class DateRangeValue(from: Option[LocalDate], to: Option[LocalDate]) extends FilterState {
    def isEmpty: Boolean = from.isEmpty && to.isEmpty
  }

  case class NumberRangeValue(min: Option[Double], max: Option[Double]) extends FilterState {
    def isEmpty: Boolean = min.isEmpty && max.isEmpty
  }

  case class BooleanValue(value: Option[Boolean]) extends FilterState {
    def isEmpty: Boolean = value.isEmpty
  }

  def emptyText: TextValue               = TextValue("")
  def emptySelect: SelectValue           = SelectValue(None)
  def emptyMultiSelect: MultiSelectValue = MultiSelectValue(Set.empty)
  def emptyDateRange: DateRangeValue     = DateRangeValue(None, None)
  def emptyNumberRange: NumberRangeValue = NumberRangeValue(None, None)
  def emptyBoolean: BooleanValue         = BooleanValue(None)
}
