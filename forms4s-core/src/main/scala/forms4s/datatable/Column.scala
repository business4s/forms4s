package forms4s.datatable

/** Column definition with type-safe value extraction.
  *
  * @tparam T
  *   Row type
  * @tparam V
  *   Value type for this column
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

  /** Change the label */
  def withLabel(newLabel: String): Column[T, V] = copy(label = newLabel)
}

object Column {

  /** Create a column with default rendering */
  def apply[T, V](id: String, label: String, extract: T => V): Column[T, V] =
    new Column(id, label, extract)
}
