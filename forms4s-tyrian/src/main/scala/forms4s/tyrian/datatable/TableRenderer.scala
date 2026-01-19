package forms4s.tyrian.datatable

import forms4s.datatable.*
import tyrian.Html

/** Abstract rendering interface for datatables.
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

  /** Render a single filter input based on filter type */
  def renderFilterInput[T, V](
      state: TableState[T],
      column: Column[T, V],
      filter: ColumnFilter[V],
      currentState: FilterState,
  ): Html[TableUpdate]
}
