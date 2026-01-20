package forms4s.datatable

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate

import scala.util.Try

/** Parsed table state from query parameters.
  *
  * This is an intermediate representation that can be applied to any TableState.
  */
case class TableStateParams(
    filters: Map[String, FilterState] = Map.empty,
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
  *   - `f.{columnId}={value}` - Text or select filter
  *   - `f.{columnId}={value}` (repeated) - Multi-select filter
  *   - `f.{columnId}=true|false` - Boolean filter
  *   - `f.{columnId}.min={n}` / `f.{columnId}.max={n}` - Number range filter
  *   - `f.{columnId}.from={date}` / `f.{columnId}.to={date}` - Date range filter
  */
object TableStateQueryParams {

  private val FilterPrefix = "f."
  private val SortKey      = "sort"
  private val PageKey      = "page"
  private val SizeKey      = "size"

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
    * @param state
    *   The table state to update
    * @param params
    *   The parsed parameters
    * @return
    *   Updated table state
    */
  def applyToState[T](state: TableState[T], params: TableStateParams): TableState[T] = {
    var updated = state

    params.filters.foreach { case (columnId, filterState) =>
      updated = updated.update(TableUpdate.SetFilter(columnId, filterState))
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
        val minParam = min.map(n => s"$FilterPrefix$columnId.min" -> formatNumber(n))
        val maxParam = max.map(n => s"$FilterPrefix$columnId.max" -> formatNumber(n))
        (minParam.toSeq ++ maxParam.toSeq)

      case FilterState.DateRangeValue(from, to) =>
        val fromParam = from.map(d => s"$FilterPrefix$columnId.from" -> d.toString)
        val toParam   = to.map(d => s"$FilterPrefix$columnId.to" -> d.toString)
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

  private def parseFilters(grouped: Map[String, Seq[String]]): Map[String, FilterState] = {
    val filterParams = grouped.filter(_._1.startsWith(FilterPrefix))

    val baseFilters    = scala.collection.mutable.Map[String, FilterState]()
    val rangeMinValues = scala.collection.mutable.Map[String, Double]()
    val rangeMaxValues = scala.collection.mutable.Map[String, Double]()
    val dateFromValues = scala.collection.mutable.Map[String, LocalDate]()
    val dateToValues   = scala.collection.mutable.Map[String, LocalDate]()

    filterParams.foreach { case (key, values) =>
      val filterKey = key.stripPrefix(FilterPrefix)

      if (filterKey.endsWith(".min")) {
        val columnId = filterKey.stripSuffix(".min")
        values.headOption.flatMap(_.toDoubleOption).foreach { n =>
          rangeMinValues(columnId) = n
        }
      } else if (filterKey.endsWith(".max")) {
        val columnId = filterKey.stripSuffix(".max")
        values.headOption.flatMap(_.toDoubleOption).foreach { n =>
          rangeMaxValues(columnId) = n
        }
      } else if (filterKey.endsWith(".from")) {
        val columnId = filterKey.stripSuffix(".from")
        values.headOption.flatMap(parseDate).foreach { d =>
          dateFromValues(columnId) = d
        }
      } else if (filterKey.endsWith(".to")) {
        val columnId = filterKey.stripSuffix(".to")
        values.headOption.flatMap(parseDate).foreach { d =>
          dateToValues(columnId) = d
        }
      } else {
        val columnId = filterKey
        if (values.size > 1) {
          baseFilters(columnId) = FilterState.MultiSelectValue(values.toSet)
        } else {
          values.headOption.foreach { v =>
            v.toLowerCase match {
              case "true"  => baseFilters(columnId) = FilterState.BooleanValue(Some(true))
              case "false" => baseFilters(columnId) = FilterState.BooleanValue(Some(false))
              case _       => baseFilters(columnId) = FilterState.TextValue(v)
            }
          }
        }
      }
    }

    val rangeColumns = rangeMinValues.keySet ++ rangeMaxValues.keySet
    rangeColumns.foreach { columnId =>
      baseFilters(columnId) = FilterState.NumberRangeValue(
        rangeMinValues.get(columnId),
        rangeMaxValues.get(columnId),
      )
    }

    val dateColumns = dateFromValues.keySet ++ dateToValues.keySet
    dateColumns.foreach { columnId =>
      baseFilters(columnId) = FilterState.DateRangeValue(
        dateFromValues.get(columnId),
        dateToValues.get(columnId),
      )
    }

    baseFilters.toMap
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
