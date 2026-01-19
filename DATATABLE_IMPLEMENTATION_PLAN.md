# Datatable Implementation Plan

## Overview

Implement a datatables.net-like component within the existing forms4s modules under `forms4s.datatable` package.

## File Structure

```
forms4s-core/src/main/scala/forms4s/datatable/
├── Column.scala              # Column definition with flexible configuration
├── ColumnFilter.scala        # Filter type hierarchy (text, select, date range, etc.)
├── TableDef.scala            # Static table definition
├── TableState.scala          # Runtime state management
├── TableUpdate.scala         # State transition messages
├── TableExport.scala         # Export functionality (CSV, JSON)
├── SortDirection.scala       # Sort enum
└── derivation/
    └── TableDerivation.scala # Compile-time derivation with customization

forms4s-tyrian/src/main/scala/forms4s/tyrian/datatable/
├── TableRenderer.scala       # Abstract rendering interface
├── RawTableRenderer.scala    # Basic HTML renderer
├── BulmaTableRenderer.scala  # Bulma CSS framework
└── BootstrapTableRenderer.scala # Bootstrap 5

forms4s-examples/src/main/scala/forms4s/example/
└── datatable/
    └── DatatableExample.scala # Example usage

website/docs/
└── datatable.md              # Documentation
```

---

## Phase 1: Core Model (forms4s-core)

### 1.1 Column.scala

```scala
package forms4s.datatable

/**
 * Column definition with type-safe value extraction.
 *
 * @tparam T Row type
 * @tparam V Value type for this column
 */
case class Column[T, V](
  id: String,
  label: String,
  extract: T => V,
  render: V => String = (v: V) => String.valueOf(v),
  sortable: Boolean = true,
  filter: Option[ColumnFilter[V]] = None,
  sortBy: Option[Ordering[V]] = None,
) {
  /** Create a copy with a different filter */
  def withFilter(f: ColumnFilter[V]): Column[T, V] = copy(filter = Some(f))

  /** Create a copy with custom rendering */
  def withRender(r: V => String): Column[T, V] = copy(render = r)

  /** Create a copy with custom sorting */
  def withSort(ord: Ordering[V]): Column[T, V] = copy(sortBy = Some(ord))

  /** Disable sorting */
  def noSort: Column[T, V] = copy(sortable = false)
}
```

### 1.2 ColumnFilter.scala

```scala
package forms4s.datatable

import java.time.LocalDate

/**
 * Filter types for columns. Each filter type has its own state and matching logic.
 */
sealed trait ColumnFilter[V] {
  type State 
  /** Test if a value matches this filter's current state */
  def matches(value: V, state: State): Boolean

  /** The filter type identifier */
  def filterType: FilterType
}

/** Filter type enumeration for rendering purposes */
enum FilterType:
  case Text
  case Select
  case MultiSelect
  case DateRange
  case NumberRange
  case Boolean

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

  val empty: TextValue = TextValue("")
}

object ColumnFilter {

  /**
   * Free text filter - matches if rendered value contains the search string (case-insensitive).
   */
  case class TextFilter[V](
    render: V => String = (v: V) => String.valueOf(v),
    caseSensitive: Boolean = false
  ) extends ColumnFilter[V] {
    type State = FilterState.TextValue
    def filterType: FilterType = FilterType.Text

    def matches(value: V, state: State): Boolean = {
      val rendered = render(value)
      if caseSensitive then rendered.contains (search)
      else rendered.toLowerCase.contains (search.toLowerCase)
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
    type State = FilterState.SelectValue
    def filterType: FilterType = FilterType.Select

    def matches(value: V, state: State): Boolean = { render(value) == selected }
  }

  /**
   * Multi-select filter - allows selecting multiple values.
   */
  case class MultiSelectFilter[V](
    render: V => String = (v: V) => String.valueOf(v)
  ) extends ColumnFilter[V] {
    type State = FilterState.MultiSelectValue
    def filterType: FilterType = FilterType.MultiSelect

    def matches(value: V, state: State): Boolean = selected.contains(render(value))
  }

  /**
   * Date range filter - filters values between from and to dates.
   */
  case class DateRangeFilter[V](
    extract: V => Option[LocalDate]
  ) extends ColumnFilter[V] {
    type State = FilterState.DateRangeValue
    def filterType: FilterType = FilterType.DateRange

    def matches(value: V, state: State): Boolean = {
      extract(value) match {
        case None => true // No date = no filter
        case Some(date) =>
          val afterFrom = from.forall(f => !date.isBefore(f))
          val beforeTo = to.forall(t => !date.isAfter(t))
          afterFrom && beforeTo
      }
    }
  }

  /**
   * Number range filter - filters numeric values between min and max.
   */
  case class NumberRangeFilter[V](
    extract: V => Option[Double]
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.NumberRange

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.NumberRangeValue(min, max) =>
        extract(value) match {
          case None => true
          case Some(num) =>
            val aboveMin = min.forall(m => num >= m)
            val belowMax = max.forall(m => num <= m)
            aboveMin && belowMax
        }
      case _ => true
    }
  }

  /**
   * Boolean filter - filters true/false/all.
   */
  case class BooleanFilter[V](
    extract: V => Boolean
  ) extends ColumnFilter[V] {
    def filterType: FilterType = FilterType.Boolean

    def matches(value: V, state: FilterState): Boolean = state match {
      case FilterState.BooleanValue(Some(expected)) => extract(value) == expected
      case _ => true
    }
  }

  /**
   * Custom filter - user-provided matching function.
   */
  case class CustomFilter[V, S <: FilterState](
    filterType: FilterType,
    matchFn: (V, S) => Boolean
  ) extends ColumnFilter[V] {
    def matches(value: V, state: FilterState): Boolean = state match {
      case s: S @unchecked => matchFn(value, s)
      case _ => true
    }
  }

  // Convenience constructors
  def text[V]: TextFilter[V] = TextFilter[V]()
  def text[V](render: V => String): TextFilter[V] = TextFilter[V](render)
  def select[V]: SelectFilter[V] = SelectFilter[V]()
  def multiSelect[V]: MultiSelectFilter[V] = MultiSelectFilter[V]()
  def dateRange[V](extract: V => Option[LocalDate]): DateRangeFilter[V] = DateRangeFilter(extract)
  def numberRange[V](extract: V => Option[Double]): NumberRangeFilter[V] = NumberRangeFilter(extract)
  def boolean[V](extract: V => Boolean): BooleanFilter[V] = BooleanFilter(extract)
}
```

### 1.3 SortDirection.scala

```scala
package forms4s.datatable

enum SortDirection:
  case Asc, Desc

  def toggle: SortDirection = this match
    case Asc => Desc
    case Desc => Asc

  def symbol: String = this match
    case Asc => "▲"
    case Desc => "▼"
```

### 1.4 TableDef.scala

```scala
package forms4s.datatable

/**
 * Static table definition - the structure of a datatable.
 *
 * @tparam T The row type
 */
case class TableDef[T](
  id: String,
  columns: List[Column[T, ?]],
  pageSize: Int = 10,
  pageSizeOptions: List[Int] = List(10, 25, 50, 100),
  selectable: Boolean = false,
  multiSelect: Boolean = false
) {

  /** Builder-style methods */
  def withPageSize(size: Int): TableDef[T] = copy(pageSize = size)
  def withPageSizeOptions(opts: List[Int]): TableDef[T] = copy(pageSizeOptions = opts)
  def withSelection(multi: Boolean = false): TableDef[T] = copy(selectable = true, multiSelect = multi)

  /** Add a column */
  def addColumn[V](col: Column[T, V]): TableDef[T] = copy(columns = columns :+ col)

  /** Remove a column by id */
  def removeColumn(id: String): TableDef[T] = copy(columns = columns.filterNot(_.id == id))

  /** Modify a column by id */
  def modifyColumn(id: String)(f: Column[T, ?] => Column[T, ?]): TableDef[T] =
    copy(columns = columns.map(c => if c.id == id then f(c) else c))
}

object TableDef {
  /** Create an empty table definition */
  def apply[T](id: String): TableDef[T] = TableDef(id, columns = Nil)
}
```

### 1.5 TableState.scala

```scala
package forms4s.datatable

/**
 * Pagination state.
 */
case class PageState(
  currentPage: Int,
  pageSize: Int
) {
  def offset: Int = currentPage * pageSize

  def totalPages(totalItems: Int): Int =
    if totalItems == 0 then 1
    else math.ceil(totalItems.toDouble / pageSize).toInt
}

/**
 * Sort state.
 */
case class SortState(
  columnId: String,
  direction: SortDirection
)

/**
 * Complete runtime state for a datatable.
 *
 * @tparam T The row type
 */
case class TableState[T](
  definition: TableDef[T],
  data: Vector[T],
  filters: Map[String, FilterState] = Map.empty,
  sort: Option[SortState] = None,
  page: PageState,
  selection: Set[Int] = Set.empty
) {

  /** Apply all filters to the data */
  def filteredData: Vector[T] = {
    if filters.isEmpty || filters.values.forall(_.isEmpty) then data
    else data.filter { row =>
      definition.columns.forall { col =>
        col.filter match {
          case None => true
          case Some(filter) =>
            filters.get(col.id) match {
              case None => true
              case Some(state) if state.isEmpty => true
              case Some(state) =>
                val value = col.extract(row)
                filter.asInstanceOf[ColumnFilter[Any]].matches(value, state)
            }
        }
      }
    }
  }

  /** Apply sorting to filtered data */
  def sortedData: Vector[T] = {
    sort match {
      case None => filteredData
      case Some(SortState(columnId, direction)) =>
        definition.columns.find(_.id == columnId) match {
          case None => filteredData
          case Some(col) =>
            val sorted = col.sortBy match {
              case Some(ord) =>
                filteredData.sortBy(row => col.extract(row))(ord.asInstanceOf[Ordering[Any]])
              case None =>
                filteredData.sortBy(row => col.render(col.extract(row)))
            }
            direction match {
              case SortDirection.Asc => sorted
              case SortDirection.Desc => sorted.reverse
            }
        }
    }
  }

  /** Apply pagination to sorted data */
  def pagedData: Vector[T] =
    sortedData.slice(page.offset, page.offset + page.pageSize)

  /** Data to display (filtered, sorted, paged) */
  def displayData: Vector[T] = pagedData

  /** Total items after filtering */
  def totalFilteredItems: Int = filteredData.size

  /** Total pages */
  def totalPages: Int = page.totalPages(totalFilteredItems)

  /** Get unique values for a select filter column */
  def uniqueValuesFor(columnId: String): List[String] = {
    definition.columns.find(_.id == columnId) match {
      case None => Nil
      case Some(col) =>
        data.map(row => col.render(col.extract(row))).distinct.sorted.toList
    }
  }

  /** Update state based on a message */
  def update(msg: TableUpdate): TableState[T] = msg match {
    // Filtering
    case TableUpdate.SetFilter(columnId, state) =>
      if state.isEmpty then copy(filters = filters - columnId, page = page.copy(currentPage = 0))
      else copy(filters = filters + (columnId -> state), page = page.copy(currentPage = 0))

    case TableUpdate.ClearFilter(columnId) =>
      copy(filters = filters - columnId, page = page.copy(currentPage = 0))

    case TableUpdate.ClearAllFilters =>
      copy(filters = Map.empty, page = page.copy(currentPage = 0))

    // Sorting
    case TableUpdate.SetSort(columnId, direction) =>
      copy(sort = Some(SortState(columnId, direction)))

    case TableUpdate.ToggleSort(columnId) =>
      sort match {
        case Some(SortState(`columnId`, dir)) =>
          copy(sort = Some(SortState(columnId, dir.toggle)))
        case _ =>
          copy(sort = Some(SortState(columnId, SortDirection.Asc)))
      }

    case TableUpdate.ClearSort =>
      copy(sort = None)

    // Pagination
    case TableUpdate.SetPage(pageNum) =>
      val maxPage = (totalPages - 1).max(0)
      copy(page = page.copy(currentPage = pageNum.max(0).min(maxPage)))

    case TableUpdate.NextPage =>
      update(TableUpdate.SetPage(page.currentPage + 1))

    case TableUpdate.PrevPage =>
      update(TableUpdate.SetPage(page.currentPage - 1))

    case TableUpdate.FirstPage =>
      update(TableUpdate.SetPage(0))

    case TableUpdate.LastPage =>
      update(TableUpdate.SetPage(totalPages - 1))

    case TableUpdate.SetPageSize(size) =>
      copy(page = PageState(0, size))

    // Selection
    case TableUpdate.SelectRow(index) =>
      if definition.multiSelect then copy(selection = selection + index)
      else copy(selection = Set(index))

    case TableUpdate.DeselectRow(index) =>
      copy(selection = selection - index)

    case TableUpdate.ToggleRowSelection(index) =>
      if selection.contains(index) then update(TableUpdate.DeselectRow(index))
      else update(TableUpdate.SelectRow(index))

    case TableUpdate.SelectAll =>
      copy(selection = (0 until totalFilteredItems).toSet)

    case TableUpdate.DeselectAll =>
      copy(selection = Set.empty)

    // Data
    case TableUpdate.SetData(newData) =>
      copy(
        data = newData.asInstanceOf[Vector[T]],
        selection = Set.empty,
        page = page.copy(currentPage = 0)
      )
  }

  /** Get selected items from filtered data */
  def selectedItems: Vector[T] = {
    val filtered = filteredData
    selection.toVector.sorted.flatMap(i => filtered.lift(i))
  }
}

object TableState {
  /** Create initial state from definition and data */
  def apply[T](definition: TableDef[T], data: Seq[T]): TableState[T] =
    TableState(
      definition = definition,
      data = data.toVector,
      page = PageState(0, definition.pageSize)
    )

  /** Create empty state from definition */
  def empty[T](definition: TableDef[T]): TableState[T] =
    apply(definition, Vector.empty)
}
```

### 1.6 TableUpdate.scala

```scala
package forms4s.datatable

/**
 * Update messages for table state transitions.
 */
enum TableUpdate:
  // Filtering
  case SetFilter(columnId: String, state: FilterState)
  case ClearFilter(columnId: String)
  case ClearAllFilters

  // Sorting
  case SetSort(columnId: String, direction: SortDirection)
  case ToggleSort(columnId: String)
  case ClearSort

  // Pagination
  case SetPage(page: Int)
  case NextPage
  case PrevPage
  case FirstPage
  case LastPage
  case SetPageSize(size: Int)

  // Selection
  case SelectRow(index: Int)
  case DeselectRow(index: Int)
  case ToggleRowSelection(index: Int)
  case SelectAll
  case DeselectAll

  // Data
  case SetData(data: Vector[Any])
```

### 1.7 TableExport.scala

```scala
package forms4s.datatable

import scala.collection.mutable.StringBuilder

/**
 * Export functionality for tables.
 */
object TableExport {

  /**
   * Export table data to CSV format.
   *
   * @param state The table state to export
   * @param includeHeaders Whether to include column headers
   * @param exportFiltered Whether to export only filtered data or all data
   * @param delimiter CSV delimiter (default comma)
   */
  def toCSV[T](
    state: TableState[T],
    includeHeaders: Boolean = true,
    exportFiltered: Boolean = true,
    delimiter: String = ","
  ): String = {
    val columns = state.definition.exportableColumns
    val data = if exportFiltered then state.filteredData else state.data

    val sb = new StringBuilder

    // Headers
    if includeHeaders then
      sb.append(columns.map(c => escapeCSV(c.label)).mkString(delimiter))
      sb.append("\n")

    // Data rows
    data.foreach { row =>
      val values = columns.map { col =>
        val value = col.extract(row)
        escapeCSV(col.render(value))
      }
      sb.append(values.mkString(delimiter))
      sb.append("\n")
    }

    sb.toString
  }
  

  /**
   * Export selected rows only.
   */
  def selectedToCSV[T](state: TableState[T], delimiter: String = ","): String = {
    val columns = state.definition.exportableColumns
    val data = state.selectedItems

    val sb = new StringBuilder
    sb.append(columns.map(c => escapeCSV(c.label)).mkString(delimiter))
    sb.append("\n")

    data.foreach { row =>
      val values = columns.map { col =>
        val value = col.extract(row)
        escapeCSV(col.render(value))
      }
      sb.append(values.mkString(delimiter))
      sb.append("\n")
    }

    sb.toString
  }

  private def escapeCSV(value: String): String = {
    if value.contains(",") || value.contains("\"") || value.contains("\n") then
      "\"" + value.replace("\"", "\"\"") + "\""
    else value
  }

  private def escapeJSON(value: String): String =
    value
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace("\t", "\\t")
}
```

### 1.8 derivation/TableDerivation.scala

```scala
package forms4s.datatable.derivation

import forms4s.datatable.*
import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror

/**
 * Typeclass for deriving table definitions from case classes.
 */
trait ToTableDef[T] {
  def columns: List[Column[T, ?]]
  def tableDef(id: String): TableDef[T] = TableDef(id, columns = columns)
}

object ToTableDef {

  /**
   * Derive a ToTableDef instance for a case class.
   */
  inline def derived[T](using m: Mirror.ProductOf[T]): ToTableDef[T] = {
    val labels = elemLabels[m.MirroredElemLabels]
    val cols = deriveColumns[T, m.MirroredElemTypes](labels)
    new ToTableDef[T] {
      def columns: List[Column[T, ?]] = cols
    }
  }

  inline def elemLabels[L <: Tuple]: List[String] =
    inline erasedValue[L] match {
      case _: EmptyTuple => Nil
      case _: (h *: t) => constValue[h].toString :: elemLabels[t]
    }

  inline def deriveColumns[T, Fields <: Tuple](labels: List[String]): List[Column[T, ?]] =
    inline erasedValue[Fields] match {
      case _: EmptyTuple => Nil
      case _: (h *: t) =>
        val label = labels.head
        val humanLabel = camelToTitle(label)
        // We need to build a column that extracts this field
        // This requires macro magic in actual implementation
        deriveColumns[T, t](labels.tail)
    }

  private def camelToTitle(s: String): String =
    s.replaceAll("([a-z])([A-Z])", "$1 $2").capitalize
}

/**
 * Builder for customizing derived table definitions.
 *
 * Usage:
 * 
 * case class User(id: Int, name: String, email: String, password: String)
 *
 * val tableDef = TableDefBuilder[User]
 *   .exclude(_.password)           // Exclude password column
 *   .modify(_.name)(_.withFilter(ColumnFilter.text))
 *   .modify(_.email)(_.withRender(e => e.take(20) + "..."))
 *   .rename(_.id, "User ID")
 *   .build("users")
 * 
 */
case class TableDefBuilder[T](
  private val exclusions: Set[String] = Set.empty,
  private val modifications: Map[String, Column[T, ?] => Column[T, ?]] = Map.empty,
  private val renames: Map[String, String] = Map.empty,
  private val additions: List[Column[T, ?]] = Nil
)(using m: Mirror.ProductOf[T]) {

  /**
   * Exclude a field from the table.
   * Uses a lambda to select the field in a type-safe way.
   */
  inline def exclude[V](inline selector: T => V): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(exclusions = exclusions + fieldName)
  }

  /**
   * Modify a column configuration.
   */
  inline def modify[V](inline selector: T => V)(f: Column[T, V] => Column[T, V]): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(modifications = modifications + (fieldName -> f.asInstanceOf[Column[T, ?] => Column[T, ?]]))
  }

  /**
   * Rename a column.
   */
  inline def rename[V](inline selector: T => V, newLabel: String): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(renames = renames + (fieldName -> newLabel))
  }

  /**
   * Add a computed column.
   */
  def addColumn[V](col: Column[T, V]): TableDefBuilder[T] =
    copy(additions = additions :+ col)

  /**
   * Build the final TableDef.
   */
  inline def build(id: String): TableDef[T] = {
    val baseColumns = deriveColumnsWithMacro[T]
    val filteredColumns = baseColumns.filterNot(c => exclusions.contains(c.id))
    val modifiedColumns = filteredColumns.map { col =>
      val renamed = renames.get(col.id).fold(col)(newLabel =>
        col.asInstanceOf[Column[T, Any]].copy(label = newLabel).asInstanceOf[Column[T, ?]]
      )
      modifications.get(col.id).fold(renamed)(f => f(renamed))
    }
    TableDef(id, columns = modifiedColumns ++ additions)
  }

  // Macro helper to extract field name from lambda
  private inline def extractFieldName[V](inline selector: T => V): String =
    ${ TableDerivationMacros.extractFieldNameImpl[T, V]('selector) }

  // Macro helper to derive columns
  private inline def deriveColumnsWithMacro[T]: List[Column[T, ?]] =
    ${ TableDerivationMacros.deriveColumnsImpl[T] }
}

object TableDefBuilder {
  inline def apply[T](using m: Mirror.ProductOf[T]): TableDefBuilder[T] =
    new TableDefBuilder[T]()
}
```

### 1.9 derivation/TableDerivationMacros.scala

```scala
package forms4s.datatable.derivation

import forms4s.datatable.*
import scala.quoted.*

object TableDerivationMacros {

  /**
   * Extract field name from a selector lambda like `_.fieldName`
   */
  def extractFieldNameImpl[T: Type, V: Type](selector: Expr[T => V])(using Quotes): Expr[String] = {
    import quotes.reflect.*

    selector.asTerm match {
      case Inlined(_, _, Block(List(DefDef(_, _, _, Some(Select(_, fieldName)))), _)) =>
        Expr(fieldName)
      case Inlined(_, _, Block(List(DefDef(_, _, _, Some(Inlined(_, _, Select(_, fieldName))))), _)) =>
        Expr(fieldName)
      case other =>
        report.errorAndAbort(s"Expected a simple field selector like `_.fieldName`, got: ${other.show}")
    }
  }

  /**
   * Derive columns for all fields of a case class.
   */
  def deriveColumnsImpl[T: Type](using Quotes): Expr[List[Column[T, ?]]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val sym = tpe.typeSymbol

    if !sym.flags.is(Flags.Case) then
      report.errorAndAbort(s"TableDefBuilder requires a case class, got ${sym.name}")

    val fields = sym.caseFields

    val columns: List[Expr[Column[T, ?]]] = fields.map { field =>
      val fieldName = field.name
      val fieldLabel = camelToTitle(fieldName)

      field.tree match {
        case ValDef(_, tpt, _) =>
          val fieldType = tpt.tpe
          fieldType.asType match {
            case '[ft] =>
              '{
                Column[T, ft](
                  id = ${Expr(fieldName)},
                  label = ${Expr(fieldLabel)},
                  extract = (t: T) => ${Select.unique('t.asTerm, fieldName).asExprOf[ft]}
                )
              }
          }
      }
    }

    Expr.ofList(columns)
  }

  private def camelToTitle(s: String): String =
    s.replaceAll("([a-z])([A-Z])", "$1 $2").capitalize
}
```

---

## Phase 2: Rendering (forms4s-tyrian)

### 2.1 TableRenderer.scala

```scala
package forms4s.tyrian.datatable

import forms4s.datatable.*
import tyrian.Html

/**
 * Abstract rendering interface for datatables.
 */
trait TableRenderer {

  /** Render complete table with all controls */
  def renderTable[T](state: TableState[T]): Html[TableUpdate]

  /** Render filter controls */
  def renderFilters[T](state: TableState[T]): Html[TableUpdate]

  /** Render table header */
  def renderHeader[T](state: TableState[T]): Html[TableUpdate]

  /** Render table body */
  def renderBody[T](state: TableState[T]): Html[TableUpdate]

  /** Render pagination controls */
  def renderPagination[T](state: TableState[T]): Html[TableUpdate]

  /** Render info text (showing X of Y) */
  def renderInfo[T](state: TableState[T]): Html[TableUpdate]

  /** Render export controls */
  def renderExportControls[T](state: TableState[T]): Html[TableUpdate]

  /** Render a single filter input based on filter type */
  def renderFilterInput[T, V](
    state: TableState[T],
    column: Column[T, V],
    filter: ColumnFilter[V],
    currentState: FilterState
  ): Html[TableUpdate]
}
```

### 2.2 RawTableRenderer.scala

Basic unstyled implementation (see design doc for full code).

### 2.3 BulmaTableRenderer.scala

Bulma-styled implementation with:
- Box container
- Level layout for info/filters
- Table with Bulma classes
- Pagination component
- Select/checkbox styling

### 2.4 BootstrapTableRenderer.scala

Bootstrap 5 implementation with:
- Card container
- Row/col layout
- Table with Bootstrap classes
- Pagination component
- Form controls styling

---

## Phase 3: Examples (forms4s-examples)

### 3.1 DatatableExample.scala

```scala
package forms4s.example.datatable

import forms4s.datatable.*
import forms4s.datatable.derivation.*
import forms4s.tyrian.datatable.*
import tyrian.*
import tyrian.Html.*
import java.time.LocalDate

// Data model
case class Employee(
  id: Int,
  name: String,
  email: String,
  department: String,
  salary: Double,
  hireDate: LocalDate,
  active: Boolean
)

object DatatableExample extends TyrianApp[Msg, Model]:

  enum Msg:
    case TableMsg(msg: TableUpdate)
    case ExportCSV
    case ExportJSON

  case class Model(
    tableState: TableState[Employee]
  )

  // Sample data
  val employees: Vector[Employee] = Vector(
    Employee(1, "Alice Smith", "alice@company.com", "Engineering", 95000, LocalDate.of(2020, 3, 15), true),
    Employee(2, "Bob Johnson", "bob@company.com", "Marketing", 75000, LocalDate.of(2019, 7, 22), true),
    Employee(3, "Carol Williams", "carol@company.com", "Engineering", 105000, LocalDate.of(2018, 1, 10), true),
    Employee(4, "David Brown", "david@company.com", "Sales", 65000, LocalDate.of(2021, 11, 5), false),
    // ... more data
  )

  // Table definition with customization
  val tableDef: TableDef[Employee] = TableDefBuilder[Employee]
    .exclude(_.id)  // Hide internal ID
    .modify(_.name)(_.withFilter(ColumnFilter.text))
    .modify(_.email)(_.withFilter(ColumnFilter.text).withRender(e => e.split("@").head + "@..."))
    .modify(_.department)(_.withFilter(ColumnFilter.select))
    .modify(_.salary)(
      _.withFilter(ColumnFilter.numberRange(s => Some(s)))
       .withRender(s => f"$$${s}%,.0f")
       .withSort(Ordering.Double.TotalOrdering)
    )
    .modify(_.hireDate)(
      _.withFilter(ColumnFilter.dateRange(d => Some(d)))
       .withRender(_.toString)
    )
    .modify(_.active)(_.withFilter(ColumnFilter.boolean(identity)))
    .rename(_.hireDate, "Hire Date")
    .build("employees")
    .withTitle("Employee Directory")
    .withSelection(multi = true)

  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(TableState(tableDef, employees)), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Msg.TableMsg(msg) =>
      (model.copy(tableState = model.tableState.update(msg)), Cmd.None)

    case Msg.ExportCSV =>
      val csv = TableExport.toCSV(model.tableState)
      // Trigger download...
      (model, Cmd.None)

    case Msg.ExportJSON =>
      val json = TableExport.toJSON(model.tableState)
      // Trigger download...
      (model, Cmd.None)

  def view(model: Model): Html[Msg] =
    div(cls := "container")(
      div(cls := "buttons")(
        button(onClick(Msg.ExportCSV))(text("Export CSV")),
        button(onClick(Msg.ExportJSON))(text("Export JSON"))
      ),
      BulmaTableRenderer
        .renderTable(model.tableState)
        .map(Msg.TableMsg.apply)
    )

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.None
```

---

## Phase 4: Documentation (website/docs)

### 4.1 datatable.md

```markdown
# Datatable

forms4s provides a powerful datatable component for displaying tabular data with filtering, sorting, pagination, and export capabilities.

## Quick Start

```scala
import forms4s.datatable.*
import forms4s.datatable.derivation.*
import forms4s.tyrian.datatable.*

case class User(name: String, email: String, role: String)

// Derive table definition from case class
val tableDef = TableDefBuilder[User].build("users")

// Create state with data
val state = TableState(tableDef, myUsers)

// Render with Bulma styling
val html = BulmaTableRenderer.renderTable(state)
```

## Table Definition

### Basic Definition

```scala
val tableDef = TableDef[User](
  id = "users",
  columns = List(
    Column("name", "Name", _.name),
    Column("email", "Email", _.email),
    Column("role", "Role", _.role)
  )
)
```

### Derived Definition with Customization

```scala
val tableDef = TableDefBuilder[User]
  .exclude(_.password)                    // Exclude field
  .modify(_.name)(_.withFilter(ColumnFilter.text))  // Add filter
  .modify(_.email)(_.noExport)            // Exclude from export
  .rename(_.createdAt, "Created")         // Rename column
  .build("users")
```

## Filters

### Text Filter
Free-text search within column values.

```scala
column.withFilter(ColumnFilter.text)
```

### Select Filter
Dropdown with all unique values.

```scala
column.withFilter(ColumnFilter.select)
```

### Multi-Select Filter
Select multiple values to include.

```scala
column.withFilter(ColumnFilter.multiSelect)
```

### Date Range Filter
Filter by date range.

```scala
column.withFilter(ColumnFilter.dateRange(d => Some(d)))
```

### Number Range Filter
Filter by numeric range.

```scala
column.withFilter(ColumnFilter.numberRange(n => Some(n.toDouble)))
```

### Boolean Filter
Filter true/false values.

```scala
column.withFilter(ColumnFilter.boolean(identity))
```

## Export

```scala
// Export to CSV
val csv = TableExport.toCSV(state)

// Export to JSON
val json = TableExport.toJSON(state)

// Export only selected rows
val selectedCsv = TableExport.selectedToCSV(state)
```

## Rendering

### Available Renderers

- `RawTableRenderer` - Unstyled HTML
- `BulmaTableRenderer` - Bulma CSS framework
- `BootstrapTableRenderer` - Bootstrap 5

### Tyrian Integration

```scala
enum Msg:
  case TableMsg(msg: TableUpdate)

def view(model: Model): Html[Msg] =
  BulmaTableRenderer
    .renderTable(model.tableState)
    .map(Msg.TableMsg.apply)

def update(model: Model): Msg => Model =
  case Msg.TableMsg(msg) =>
    model.copy(tableState = model.tableState.update(msg))
```
```

---

## Implementation Order

1. **Core Types** (Phase 1.1-1.6)
   - [ ] Column.scala
   - [ ] ColumnFilter.scala + FilterState
   - [ ] SortDirection.scala
   - [ ] TableDef.scala
   - [ ] TableState.scala
   - [ ] TableUpdate.scala
   - [ ] TableExport.scala

2. **Derivation** (Phase 1.8-1.9)
   - [ ] TableDerivation.scala (ToTableDef trait)
   - [ ] TableDefBuilder
   - [ ] TableDerivationMacros.scala

3. **Rendering** (Phase 2)
   - [ ] TableRenderer.scala (trait)
   - [ ] RawTableRenderer.scala
   - [ ] BulmaTableRenderer.scala
   - [ ] BootstrapTableRenderer.scala

4. **Example** (Phase 3)
   - [ ] DatatableExample.scala

5. **Documentation** (Phase 4)
   - [ ] datatable.md

---

## Key Design Decisions

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| Package | `forms4s.datatable` | Lives alongside forms, same library |
| Filter types | Sealed trait hierarchy | Type-safe, extensible, render-aware |
| Derivation | Builder pattern with macros | Allows field selection via lambda |
| Export | Standalone object | No dependencies, easy to use |
| State | Immutable with update | Fits functional architecture |
