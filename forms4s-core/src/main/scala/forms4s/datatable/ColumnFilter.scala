package forms4s.datatable

import java.time.LocalDate

/** Filter type enumeration for rendering purposes */
enum FilterType {
  case Text
  case Select
  case MultiSelect
  case DateRange
  case NumberRange
  case Boolean
}

/**
 * Filter types for columns. Each filter type defines matching logic.
 */
sealed trait ColumnFilter[V] {

  /** The filter type identifier for rendering */
  def filterType: FilterType

  /** Test if a value matches this filter's current state */
  def matches(value: V, state: FilterState): Boolean

  /** Get the empty state for this filter type */
  def emptyState: FilterState
}

object ColumnFilter {

  /**
   * Free text filter - matches if rendered value contains the search string (case-insensitive).
   */
  case class TextFilter[V](
      render: V => String = (v: V) => String.valueOf(v),
      caseSensitive: Boolean = false
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.Text
    def emptyState: FilterState = FilterState.emptyText

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.TextValue(search) if search.nonEmpty =>
        val rendered = render(value)
        if (caseSensitive) rendered.contains(search)
        else rendered.toLowerCase.contains(search.toLowerCase)
      case FilterState.TextValue(_) => true
      case other =>
        throw new IllegalArgumentException(s"TextFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  /**
   * Select filter - dropdown with all unique values from the data.
   * Options are computed dynamically from data.
   */
  case class SelectFilter[V](
      render: V => String = (v: V) => String.valueOf(v),
      includeBlank: Boolean = true
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.Select
    def emptyState: FilterState = FilterState.emptySelect

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.SelectValue(Some(selected)) => render(value) == selected
      case FilterState.SelectValue(None)           => true
      case other =>
        throw new IllegalArgumentException(s"SelectFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  /**
   * Multi-select filter - allows selecting multiple values.
   */
  case class MultiSelectFilter[V](
      render: V => String = (v: V) => String.valueOf(v)
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.MultiSelect
    def emptyState: FilterState = FilterState.emptyMultiSelect

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.MultiSelectValue(selected) if selected.nonEmpty =>
        selected.contains(render(value))
      case FilterState.MultiSelectValue(_) => true
      case other =>
        throw new IllegalArgumentException(s"MultiSelectFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  /**
   * Date range filter - filters values between from and to dates.
   */
  case class DateRangeFilter[V](
      extract: V => Option[LocalDate]
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.DateRange
    def emptyState: FilterState = FilterState.emptyDateRange

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.DateRangeValue(from, to) if from.isDefined || to.isDefined =>
        extract(value) match {
          case None => true // No date = no filter
          case Some(date) =>
            val afterFrom = from.forall(f => !date.isBefore(f))
            val beforeTo = to.forall(t => !date.isAfter(t))
            afterFrom && beforeTo
        }
      case FilterState.DateRangeValue(_, _) => true
      case other =>
        throw new IllegalArgumentException(s"DateRangeFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  /**
   * Number range filter - filters numeric values between min and max.
   */
  case class NumberRangeFilter[V](
      extract: V => Option[Double]
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.NumberRange
    def emptyState: FilterState = FilterState.emptyNumberRange

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.NumberRangeValue(min, max) if min.isDefined || max.isDefined =>
        extract(value) match {
          case None => true
          case Some(num) =>
            val aboveMin = min.forall(m => num >= m)
            val belowMax = max.forall(m => num <= m)
            aboveMin && belowMax
        }
      case FilterState.NumberRangeValue(_, _) => true
      case other =>
        throw new IllegalArgumentException(s"NumberRangeFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  /**
   * Boolean filter - filters true/false/all.
   */
  case class BooleanFilter[V](
      extract: V => Boolean
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.Boolean
    def emptyState: FilterState = FilterState.emptyBoolean

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.BooleanValue(Some(expected)) => extract(value) == expected
      case FilterState.BooleanValue(None)           => true
      case other =>
        throw new IllegalArgumentException(s"BooleanFilter received invalid state type: ${other.getClass.getSimpleName}")
    }
  }

  // Convenience constructors
  def text[V]: TextFilter[V] = TextFilter[V]()
  def text[V](render: V => String): TextFilter[V] = TextFilter[V](render)
  def select[V]: SelectFilter[V] = SelectFilter[V]()
  def select[V](render: V => String): SelectFilter[V] = SelectFilter[V](render)
  def multiSelect[V]: MultiSelectFilter[V] = MultiSelectFilter[V]()
  def multiSelect[V](render: V => String): MultiSelectFilter[V] = MultiSelectFilter[V](render)
  def dateRange[V](extract: V => Option[LocalDate]): DateRangeFilter[V] = DateRangeFilter(extract)
  def numberRange[V](extract: V => Option[Double]): NumberRangeFilter[V] = NumberRangeFilter(extract)
  def boolean[V](extract: V => Boolean): BooleanFilter[V] = BooleanFilter(extract)
}
