package forms4s.datatable

/** Static table definition - the structure of a datatable.
  *
  * @tparam T
  *   The row type
  */
case class TableDef[T](
    id: String,
    columns: List[Column[T, ?]],
    pageSize: Int = 10,
    pageSizeOptions: List[Int] = List(10, 25, 50, 100),
    selectable: Boolean = false,
    multiSelect: Boolean = false,
) {

  /** Builder-style methods */
  def withPageSize(size: Int): TableDef[T]               = copy(pageSize = size)
  def withPageSizeOptions(opts: List[Int]): TableDef[T]  = copy(pageSizeOptions = opts)
  def withSelection(multi: Boolean = false): TableDef[T] = copy(selectable = true, multiSelect = multi)

  /** Add a column */
  def addColumn[V](col: Column[T, V]): TableDef[T] = copy(columns = columns :+ col)

  /** Remove a column by id */
  def removeColumn(id: String): TableDef[T] = copy(columns = columns.filterNot(_.id == id))

  /** Modify a column by id */
  def modifyColumn(id: String)(f: Column[T, ?] => Column[T, ?]): TableDef[T] =
    copy(columns = columns.map(c => if (c.id == id) f(c) else c))
}

object TableDef {

  /** Create an empty table definition */
  def apply[T](id: String): TableDef[T] = TableDef(id, columns = Nil)

  /** Create a table definition with columns */
  def apply[T](id: String, columns: List[Column[T, ?]]): TableDef[T] =
    new TableDef(id, columns)
}
