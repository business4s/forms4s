package forms4s.datatable

/**
 * Update messages for table state transitions.
 */
enum TableUpdate {
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
  case SetData[T](data: Vector[T])

  // Export
  case ExportCSV
}
