package forms4s.datatable

/** Update messages for table state transitions.
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

  /** Returns true if this update affects server-side data (filtering, sorting, pagination) and thus requires fetching fresh data from the server in
    * server-data mode.
    */
  def needsServerFetch: Boolean = this match {
    case _: SetFilter          => true
    case _: ClearFilter        => true
    case ClearAllFilters       => true
    case _: SetSort            => true
    case _: ToggleSort         => true
    case ClearSort             => true
    case _: SetPage            => true
    case NextPage              => true
    case PrevPage              => true
    case FirstPage             => true
    case LastPage              => true
    case _: SetPageSize        => true
    case _: SelectRow          => false
    case _: DeselectRow        => false
    case _: ToggleRowSelection => false
    case SelectAll             => false
    case DeselectAll           => false
    case _: SetData[?]         => false
    case ExportCSV             => false
  }
}
