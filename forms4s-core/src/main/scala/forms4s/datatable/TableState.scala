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
    if (totalItems == 0) 1
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
    if (filters.isEmpty || filters.values.forall(_.isEmpty)) data
    else
      data.filter { row =>
        definition.columns.forall { col =>
          col.filter match {
            case None => true
            case Some(filter) =>
              filters.get(col.id) match {
                case None                        => true
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
                filteredData.sortBy(row => col.extract(row))(using ord.asInstanceOf[Ordering[Any]])
              case None =>
                filteredData.sortBy(row => col.render(col.extract(row)))
            }
            direction match {
              case SortDirection.Asc  => sorted
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
      case None      => Nil
      case Some(col) => data.map(row => col.render(col.extract(row))).distinct.sorted.toList
    }
  }

  /** Update state based on a message */
  def update(msg: TableUpdate): TableState[T] = msg match {
    // Filtering
    case TableUpdate.SetFilter(columnId, state) =>
      if (state.isEmpty) copy(filters = filters - columnId, page = page.copy(currentPage = 0))
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
      if (definition.multiSelect) copy(selection = selection + index)
      else copy(selection = Set(index))

    case TableUpdate.DeselectRow(index) =>
      copy(selection = selection - index)

    case TableUpdate.ToggleRowSelection(index) =>
      if (selection.contains(index)) update(TableUpdate.DeselectRow(index))
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

    // Export (no state change, handled by parent)
    case TableUpdate.ExportCSV => this
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
