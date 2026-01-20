package forms4s.datatable

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

import scala.util.Try

/** Intermediate representation of parsed filter values from query parameters.
  *
  * These are raw parsed values before being converted to proper FilterState types.
  */
sealed trait ParsedFilterValue
object ParsedFilterValue {
  case class SimpleValues(values: Seq[String])                    extends ParsedFilterValue
  case class RangeValue(min: Option[String], max: Option[String]) extends ParsedFilterValue
}

/** Parsed table state from query parameters.
  *
  * This is an intermediate representation that can be applied to any TableState.
  */
case class TableStateParams(
    filters: Map[String, ParsedFilterValue] = Map.empty,
    sort: Option[SortState] = None,
    page: Option[Int] = None,
    pageSize: Option[Int] = None,
)

/** Utilities for converting table state to/from URL query parameters.
  *
  * Query parameter format:
  *   - `sort={columnId}:{asc|desc}` - Sorting
  *   - `page={0-indexed}` - Current page
  *   - `size={number}` - Page size
  *   - `f.{columnId}={value}` - Text, select, or boolean filter
  *   - `f.{columnId}={value}` (repeated) - Multi-select filter
  *   - `f.{columnId}.min={value}` / `f.{columnId}.max={value}` - Range filter (number or date)
  */
object TableStateQueryParams {

  private val FilterPrefix = "f."
  private val SortKey      = "sort"
  private val PageKey      = "page"
  private val SizeKey      = "size"
  private val MinSuffix    = ".min"
  private val MaxSuffix    = ".max"

  /** Convert table state to a URL query string.
    *
    * @param state
    *   The table state to serialize
    * @return
    *   URL query string (without leading '?'), URL-encoded
    */
  def toQueryString[T](state: TableState[T]): String = {
    val params = toQueryParams(state)
    if (params.isEmpty) ""
    else params.map { case (k, v) => s"${encode(k)}=${encode(v)}" }.mkString("&")
  }

  /** Convert table state to query parameters.
    *
    * @param state
    *   The table state to serialize
    * @return
    *   Sequence of key-value pairs (supports repeated keys for multi-select)
    */
  def toQueryParams[T](state: TableState[T]): Seq[(String, String)] = {
    val filterParams = state.filters.toSeq.flatMap { case (columnId, filterState) =>
      filterStateToParams(columnId, filterState)
    }

    val sortParams = state.sort.toSeq.map { s =>
      SortKey -> s"${s.columnId}:${s.direction.toString.toLowerCase}"
    }

    val pageParams =
      if (state.page.currentPage > 0) Seq(PageKey -> state.page.currentPage.toString)
      else Seq.empty

    val sizeParams = state.definition.pageSize match {
      case defaultSize if state.page.pageSize != defaultSize =>
        Seq(SizeKey -> state.page.pageSize.toString)
      case _                                                 => Seq.empty
    }

    sortParams ++ pageParams ++ sizeParams ++ filterParams
  }

  /** Parse query string into table state parameters.
    *
    * @param queryString
    *   URL query string (with or without leading '?')
    * @return
    *   Parsed parameters
    */
  def fromQueryString(queryString: String): TableStateParams = {
    val qs = queryString.stripPrefix("?")
    if (qs.isEmpty) return TableStateParams()

    val params = qs
      .split("&")
      .filter(_.nonEmpty)
      .flatMap { pair =>
        pair.split("=", 2) match {
          case Array(k, v) => Some(decode(k) -> decode(v))
          case Array(k)    => Some(decode(k) -> "")
          case _           => None
        }
      }
      .toSeq

    fromQueryParams(params)
  }

  /** Parse query parameters into table state parameters.
    *
    * @param params
    *   Sequence of key-value pairs
    * @return
    *   Parsed parameters
    */
  def fromQueryParams(params: Seq[(String, String)]): TableStateParams = {
    val grouped = params.groupBy(_._1).view.mapValues(_.map(_._2)).toMap

    val sort = grouped.get(SortKey).flatMap(_.headOption).flatMap(parseSortValue)

    val page     = grouped.get(PageKey).flatMap(_.headOption).flatMap(_.toIntOption).filter(_ >= 0)
    val pageSize = grouped.get(SizeKey).flatMap(_.headOption).flatMap(_.toIntOption).filter(_ > 0)

    val filters = parseFilters(grouped)

    TableStateParams(filters, sort, page, pageSize)
  }

  /** Apply parsed parameters to an existing table state.
    *
    * Uses the TableDef to determine the correct FilterState type for each column.
    *
    * @param state
    *   The table state to update
    * @param params
    *   The parsed parameters
    * @return
    *   Updated table state
    */
  def applyToState[T](state: TableState[T], params: TableStateParams): TableState[T] = {
    var updated = state

    params.filters.foreach { case (columnId, parsedValue) =>
      val filterState = toFilterState(parsedValue, state.definition, columnId)
      filterState.foreach { fs =>
        updated = updated.update(TableUpdate.SetFilter(columnId, fs))
      }
    }

    params.sort.foreach { s =>
      updated = updated.update(TableUpdate.SetSort(s.columnId, s.direction))
    }

    params.pageSize.foreach { size =>
      updated = updated.update(TableUpdate.SetPageSize(size))
    }

    params.page.foreach { p =>
      updated = updated.update(TableUpdate.SetPage(p))
    }

    updated
  }

  /** Convert a parsed filter value to the appropriate FilterState based on the column's filter type.
    *
    * Returns None if:
    *   - The column doesn't exist in the TableDef
    *   - The column has no filter defined
    *   - The parsed value type doesn't match what the filter expects
    */
  private def toFilterState[T](
      parsed: ParsedFilterValue,
      tableDef: TableDef[T],
      columnId: String,
  ): Option[FilterState] = {
    val column           = tableDef.columns.find(_.id == columnId)
    val filterType       = column.flatMap(_.filter)
    def unexpectedValues = {
      // We might want exception or log line here, but for now just ignore the values
      None
    }

    filterType match {
      case Some(_: ColumnFilter.TextFilter[_]) =>
        parsed match {
          case ParsedFilterValue.SimpleValues(values) =>
            values.headOption.map(FilterState.TextValue.apply)
          case _: ParsedFilterValue.RangeValue        => unexpectedValues
        }

      case Some(_: ColumnFilter.SelectFilter[_]) =>
        parsed match {
          case ParsedFilterValue.SimpleValues(values) =>
            values.headOption.map(v => FilterState.SelectValue(Some(v)))
          case _: ParsedFilterValue.RangeValue        => unexpectedValues
        }

      case Some(_: ColumnFilter.MultiSelectFilter[_]) =>
        parsed match {
          case ParsedFilterValue.SimpleValues(values) =>
            Some(FilterState.MultiSelectValue(values.toSet))
          case _: ParsedFilterValue.RangeValue        => unexpectedValues
        }

      case Some(_: ColumnFilter.BooleanFilter[_]) =>
        parsed match {
          case ParsedFilterValue.SimpleValues(values) =>
            values.headOption.flatMap { v =>
              v.toLowerCase match {
                case "true"  => Some(FilterState.BooleanValue(Some(true)))
                case "false" => Some(FilterState.BooleanValue(Some(false)))
                case _       => unexpectedValues
              }
            }
          case _: ParsedFilterValue.RangeValue        => unexpectedValues
        }

      case Some(_: ColumnFilter.NumberRangeFilter[_]) =>
        parsed match {
          case ParsedFilterValue.RangeValue(min, max) =>
            Some(FilterState.NumberRangeValue(min.flatMap(_.toDoubleOption), max.flatMap(_.toDoubleOption)))
          case _: ParsedFilterValue.SimpleValues      => unexpectedValues
        }

      case Some(_: ColumnFilter.DateRangeFilter[_]) =>
        parsed match {
          case ParsedFilterValue.RangeValue(min, max) =>
            Some(FilterState.DateRangeValue(min.flatMap(parseDate), max.flatMap(parseDate)))
          case _: ParsedFilterValue.SimpleValues      => unexpectedValues
        }

      case None => unexpectedValues // column not found or has no filter
    }
  }

  private def filterStateToParams(columnId: String, state: FilterState): Seq[(String, String)] = {
    state match {
      case FilterState.TextValue(v) if v.nonEmpty =>
        Seq(s"$FilterPrefix$columnId" -> v)

      case FilterState.SelectValue(Some(v)) =>
        Seq(s"$FilterPrefix$columnId" -> v)

      case FilterState.MultiSelectValue(selected) if selected.nonEmpty =>
        selected.toSeq.sorted.map(v => s"$FilterPrefix$columnId" -> v)

      case FilterState.BooleanValue(Some(b)) =>
        Seq(s"$FilterPrefix$columnId" -> b.toString)

      case FilterState.NumberRangeValue(min, max) =>
        val minParam = min.map(n => s"$FilterPrefix$columnId$MinSuffix" -> formatNumber(n))
        val maxParam = max.map(n => s"$FilterPrefix$columnId$MaxSuffix" -> formatNumber(n))
        (minParam.toSeq ++ maxParam.toSeq)

      case FilterState.DateRangeValue(from, to) =>
        val fromParam = from.map(d => s"$FilterPrefix$columnId$MinSuffix" -> d.toString)
        val toParam   = to.map(d => s"$FilterPrefix$columnId$MaxSuffix" -> d.toString)
        (fromParam.toSeq ++ toParam.toSeq)

      case _ => Seq.empty
    }
  }

  private def parseSortValue(value: String): Option[SortState] = {
    value.split(":", 2) match {
      case Array(columnId, dir) =>
        val direction = dir.toLowerCase match {
          case "asc"  => Some(SortDirection.Asc)
          case "desc" => Some(SortDirection.Desc)
          case _      => None
        }
        direction.map(d => SortState(columnId, d))
      case _                    => None
    }
  }

  private def parseFilters(grouped: Map[String, Seq[String]]): Map[String, ParsedFilterValue] = {
    val filterParams = grouped.filter(_._1.startsWith(FilterPrefix))

    val simpleValues = scala.collection.mutable.Map[String, Seq[String]]().withDefaultValue(Seq.empty)
    val rangeMin     = scala.collection.mutable.Map[String, String]()
    val rangeMax     = scala.collection.mutable.Map[String, String]()

    filterParams.foreach { case (key, values) =>
      val filterKey = key.stripPrefix(FilterPrefix)

      if (filterKey.endsWith(MinSuffix)) {
        val columnId = filterKey.stripSuffix(MinSuffix)
        values.headOption.foreach { v =>
          rangeMin(columnId) = v
        }
      } else if (filterKey.endsWith(MaxSuffix)) {
        val columnId = filterKey.stripSuffix(MaxSuffix)
        values.headOption.foreach { v =>
          rangeMax(columnId) = v
        }
      } else {
        // Simple value (text, select, multi-select, or boolean - type determined later)
        val columnId = filterKey
        simpleValues(columnId) = simpleValues(columnId) ++ values
      }
    }

    val result = scala.collection.mutable.Map[String, ParsedFilterValue]()

    // Add simple values
    simpleValues.foreach { case (columnId, values) =>
      if (values.nonEmpty) {
        result(columnId) = ParsedFilterValue.SimpleValues(values)
      }
    }

    // Add ranges (type will be determined from TableDef)
    val rangeColumns = rangeMin.keySet ++ rangeMax.keySet
    rangeColumns.foreach { columnId =>
      result(columnId) = ParsedFilterValue.RangeValue(
        rangeMin.get(columnId),
        rangeMax.get(columnId),
      )
    }

    result.toMap
  }

  private def parseDate(s: String): Option[LocalDate] =
    Try(LocalDate.parse(s)).toOption

  private def formatNumber(n: Double): String = {
    if (n == n.toLong) n.toLong.toString
    else n.toString
  }

  private def encode(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8)

  private def decode(s: String): String =
    URLDecoder.decode(s, StandardCharsets.UTF_8)
}
