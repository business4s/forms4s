package forms4s.datatable

/** Export functionality for tables.
  */
object TableExport {

  /** Export table data to CSV format.
    *
    * @param state
    *   The table state to export
    * @param includeHeaders
    *   Whether to include column headers
    * @param exportFiltered
    *   Whether to export only filtered data or all data
    * @param delimiter
    *   CSV delimiter (default comma)
    */
  def toCSV[T](
      state: TableState[T],
      includeHeaders: Boolean = true,
      exportFiltered: Boolean = true,
      delimiter: String = ",",
  ): String = {
    val columns = state.definition.columns
    val data    = if (exportFiltered) state.filteredData else state.data

    val sb = new StringBuilder

    // Headers
    if (includeHeaders) {
      sb.append(columns.map(c => escapeCSV(c.label)).mkString(delimiter))
      sb.append("\n")
    }

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

  /** Export selected rows only.
    */
  def selectedToCSV[T](state: TableState[T], delimiter: String = ","): String = {
    val columns = state.definition.columns
    val data    = state.selectedItems

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
    if (value.contains(",") || value.contains("\"") || value.contains("\n"))
      "\"" + value.replace("\"", "\"\"") + "\""
    else value
  }
}
