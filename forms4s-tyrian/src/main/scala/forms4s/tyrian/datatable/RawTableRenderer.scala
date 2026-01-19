package forms4s.tyrian.datatable

import forms4s.datatable.*
import tyrian.Html
import tyrian.Html.*

/** Basic unstyled HTML table renderer.
  */
object RawTableRenderer extends TableRenderer {

  override def renderTable[T](state: TableState[T]): Html[TableUpdate] = {
    div(
      div(
        renderInfo(state),
        button(onClick(TableUpdate.ExportCSV))("Export CSV"),
      ),
      renderFilters(state),
      Html.table(
        renderHeader(state),
        renderBody(state),
      ),
      renderPagination(state),
    )
  }

  override def renderFilters[T](state: TableState[T]): Html[TableUpdate] = {
    val filterColumns = state.definition.columns.filter(_.filter.isDefined)
    if (filterColumns.isEmpty) div()()
    else
      div(
        filterColumns.map { col =>
          col.filter match {
            case Some(filter) =>
              val currentState = state.filters.getOrElse(col.id, filter.emptyState)
              div(
                label(col.label + ": "),
                renderFilterInput(state, col.asInstanceOf[Column[T, Any]], filter.asInstanceOf[ColumnFilter[Any]], currentState),
              )
            case None         => div()()
          }
        },
      )
  }

  override def renderHeader[T](state: TableState[T]): Html[TableUpdate] = {
    Html.thead(
      tr(
        state.definition.columns.map { col =>
          val sortIndicator = state.sort match {
            case Some(SortState(colId, dir)) if colId == col.id => " " + dir.symbol
            case _                                              => ""
          }
          if (col.sortable)
            th(onClick(TableUpdate.ToggleSort(col.id)), styles("cursor" -> "pointer"))(
              col.label + sortIndicator,
            )
          else th(col.label)
        },
      ),
    )
  }

  override def renderBody[T](state: TableState[T]): Html[TableUpdate] = {
    Html.tbody(
      state.displayDataWithIndices.toList.map { case (row, originalIdx) =>
        val isSelected = state.selection.contains(originalIdx)
        val rowAttrs   = List(
          if (state.definition.selectable) Some(onClick(TableUpdate.ToggleRowSelection(originalIdx))) else None,
          if (isSelected) Some(styles("background-color" -> "#e0e0e0")) else None,
        ).flatten
        tr(rowAttrs*)(
          state.definition.columns.map { col =>
            val value = col.extract(row)
            td(col.render(value))
          },
        )
      },
    )
  }

  override def renderPagination[T](state: TableState[T]): Html[TableUpdate] = {
    val totalPages  = state.totalPages
    val currentPage = state.page.currentPage

    div(
      button(
        onClick(TableUpdate.FirstPage),
        disabled(currentPage == 0),
      )("First"),
      button(
        onClick(TableUpdate.PrevPage),
        disabled(currentPage == 0),
      )("Prev"),
      span(s" Page ${currentPage + 1} of $totalPages "),
      button(
        onClick(TableUpdate.NextPage),
        disabled(currentPage >= totalPages - 1),
      )("Next"),
      button(
        onClick(TableUpdate.LastPage),
        disabled(currentPage >= totalPages - 1),
      )("Last"),
      span(" | Page size: "),
      Html.select(
        onChange(v => TableUpdate.SetPageSize(v.toInt)),
      )(
        state.definition.pageSizeOptions.map { size =>
          option(
            value := size.toString,
            selected(state.page.pageSize == size),
          )(size.toString)
        },
      ),
    )
  }

  override def renderInfo[T](state: TableState[T]): Html[TableUpdate] = {
    val start    = state.page.offset + 1
    val end      = math.min(state.page.offset + state.page.pageSize, state.totalFilteredItems)
    val total    = state.totalFilteredItems
    val allTotal = state.data.size

    div(
      if (total == allTotal)
        s"Showing $start to $end of $total entries"
      else
        s"Showing $start to $end of $total entries (filtered from $allTotal total)",
    )
  }

  override def renderFilterInput[T, V](
      state: TableState[T],
      column: Column[T, V],
      filter: ColumnFilter[V],
      currentState: FilterState,
  ): Html[TableUpdate] = {
    filter.filterType match {
      case FilterType.Text =>
        val textValue      = currentState match {
          case FilterState.TextValue(v) => v
          case _                        => ""
        }
        input(
          `type`      := "text",
          value       := textValue,
          placeholder := s"Search ${column.label}...",
          onInput(v => TableUpdate.SetFilter(column.id, FilterState.TextValue(v))),
        )

      case FilterType.Select =>
        val selectedValue = currentState match {
          case FilterState.SelectValue(v) => v.getOrElse("")
          case _                          => ""
        }
        val options       = state.uniqueValuesFor(column.id)
        Html.select(
          onChange(v =>
            if (v.isEmpty) TableUpdate.ClearFilter(column.id)
            else TableUpdate.SetFilter(column.id, FilterState.SelectValue(Some(v))),
          ),
        )(
          option(value := "")("All") ::
            options.map(opt => option(value := opt, selected(selectedValue == opt))(opt)),
        )

      case FilterType.MultiSelect =>
        val selectedValues = currentState match {
          case FilterState.MultiSelectValue(v) => v
          case _                               => Set.empty[String]
        }
        val options        = state.uniqueValuesFor(column.id)
        div(
          options.map { opt =>
            label(
              input(
                `type` := "checkbox",
                checked(selectedValues.contains(opt)),
                onChange(_ =>
                  if (selectedValues.contains(opt))
                    TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues - opt))
                  else
                    TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues + opt)),
                ),
              ),
              span(opt),
            )
          },
        )

      case FilterType.NumberRange =>
        val (minVal, maxVal) = currentState match {
          case FilterState.NumberRangeValue(min, max) => (min, max)
          case _                                      => (None, None)
        }
        div(
          input(
            `type`      := "number",
            placeholder := "Min",
            value       := minVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.NumberRangeValue(v.toDoubleOption, maxVal),
              ),
            ),
          ),
          span(" - "),
          input(
            `type`      := "number",
            placeholder := "Max",
            value       := maxVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.NumberRangeValue(minVal, v.toDoubleOption),
              ),
            ),
          ),
        )

      case FilterType.DateRange =>
        val (fromVal, toVal) = currentState match {
          case FilterState.DateRangeValue(from, to) => (from, to)
          case _                                    => (None, None)
        }
        div(
          input(
            `type` := "date",
            value  := fromVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.DateRangeValue(
                  if (v.isEmpty) None else Some(java.time.LocalDate.parse(v)),
                  toVal,
                ),
              ),
            ),
          ),
          span(" to "),
          input(
            `type` := "date",
            value  := toVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.DateRangeValue(
                  fromVal,
                  if (v.isEmpty) None else Some(java.time.LocalDate.parse(v)),
                ),
              ),
            ),
          ),
        )

      case FilterType.Boolean =>
        val boolValue = currentState match {
          case FilterState.BooleanValue(v) => v
          case _                           => None
        }
        Html.select(
          onChange(v =>
            v match {
              case ""      => TableUpdate.ClearFilter(column.id)
              case "true"  => TableUpdate.SetFilter(column.id, FilterState.BooleanValue(Some(true)))
              case "false" => TableUpdate.SetFilter(column.id, FilterState.BooleanValue(Some(false)))
              case _       => TableUpdate.ClearFilter(column.id)
            },
          ),
        )(
          option(value := "", selected(boolValue.isEmpty))("All"),
          option(value := "true", selected(boolValue.contains(true)))("Yes"),
          option(value := "false", selected(boolValue.contains(false)))("No"),
        )
    }
  }
}
