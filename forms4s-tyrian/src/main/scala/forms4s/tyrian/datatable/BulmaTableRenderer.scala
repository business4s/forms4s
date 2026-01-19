package forms4s.tyrian.datatable

import forms4s.datatable.*
import tyrian.Html
import tyrian.Html.*

/** Bulma CSS framework styled table renderer.
  */
object BulmaTableRenderer extends TableRenderer {

  override def renderTable[T](state: TableState[T]): Html[TableUpdate] = {
    div(className := "box")(
      div(className := "level")(
        div(className := "level-left")(
          div(className := "level-item")(renderInfo(state)),
        ),
        div(className := "level-right")(
          div(className := "level-item")(renderPageSizeSelect(state)),
          div(className := "level-item")(
            button(
              className := "button is-small is-primary",
              onClick(TableUpdate.ExportCSV),
            )("Export CSV"),
          ),
        ),
      ),
      renderFilters(state),
      div(className := "table-container")(
        Html.table(className := "table is-striped is-hoverable is-fullwidth")(
          renderHeader(state),
          renderBody(state),
        ),
      ),
      renderPagination(state),
    )
  }

  override def renderFilters[T](state: TableState[T]): Html[TableUpdate] = {
    val filterColumns = state.definition.columns.filter(_.filter.isDefined)
    if (filterColumns.isEmpty) div()()
    else
      div(className := "columns is-multiline mb-4")(
        filterColumns.map { col =>
          col.filter match {
            case Some(filter) =>
              val currentState = state.filters.getOrElse(col.id, filter.emptyState)
              div(className := "column is-narrow")(
                div(className := "field")(
                  label(className := "label is-small")(col.label),
                  div(className := "control")(
                    renderFilterInput(state, col.asInstanceOf[Column[T, Any]], filter.asInstanceOf[ColumnFilter[Any]], currentState),
                  ),
                ),
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
          val sortIcon = state.sort match {
            case Some(SortState(colId, dir)) if colId == col.id =>
              span(className := "ml-1")(if (dir == SortDirection.Asc) "▲" else "▼")
            case _ if col.sortable                              =>
              span(className := "ml-1 has-text-grey-lighter")("↕")
            case _                                              => span()()
          }

          if (col.sortable)
            th(
              onClick(TableUpdate.ToggleSort(col.id)),
              styles("cursor" -> "pointer", "user-select" -> "none"),
            )(
              span(col.label),
              sortIcon,
            )
          else th(col.label)
        },
      ),
    )
  }

  override def renderBody[T](state: TableState[T]): Html[TableUpdate] = {
    Html.tbody(
      if (state.displayData.isEmpty)
        List(
          tr(
            td(attribute("colspan", state.definition.columns.size.toString))(
              div(className := "has-text-centered has-text-grey")(
                "No data available",
              ),
            ),
          ),
        )
      else
        state.displayData.zipWithIndex.toList.map { case (row, idx) =>
          val globalIdx  = state.page.offset + idx
          val isSelected = state.selection.contains(globalIdx)
          val rowAttrs   = List(
            if (state.definition.selectable) Some(onClick(TableUpdate.ToggleRowSelection(globalIdx))) else None,
            if (isSelected) Some(className := "is-selected") else None,
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

    Html.nav(className := "pagination is-centered", attribute("role", "navigation"))(
      button(
        className := "pagination-previous",
        onClick(TableUpdate.PrevPage),
        disabled(currentPage == 0),
      )("Previous"),
      button(
        className := "pagination-next",
        onClick(TableUpdate.NextPage),
        disabled(currentPage >= totalPages - 1),
      )("Next"),
      Html.ul(className := "pagination-list")(
        renderPaginationNumbers(currentPage, totalPages),
      ),
    )
  }

  private def renderPaginationNumbers(current: Int, total: Int): List[Html[TableUpdate]] = {
    def addPage(n: Int): Html[TableUpdate] = {
      Html.li(
        a(
          className := s"pagination-link${if (n == current) " is-current" else ""}",
          onClick(TableUpdate.SetPage(n)),
        )((n + 1).toString),
      )
    }

    def addEllipsis(): Html[TableUpdate] = {
      Html.li(
        span(className := "pagination-ellipsis")("..."),
      )
    }

    if (total <= 7) {
      // Show all pages
      (0 until total).toList.map(addPage)
    } else {
      val pages = scala.collection.mutable.ListBuffer[Html[TableUpdate]]()

      // Always show first page
      pages += addPage(0)

      if (current > 2) pages += addEllipsis()

      // Show pages around current
      val start = math.max(1, current - 1)
      val end   = math.min(total - 2, current + 1)
      (start to end).foreach(n => pages += addPage(n))

      if (current < total - 3) pages += addEllipsis()

      // Always show last page
      if (total > 1) pages += addPage(total - 1)

      pages.toList
    }
  }

  private def renderPageSizeSelect[T](state: TableState[T]): Html[TableUpdate] = {
    div(className := "field has-addons")(
      div(className := "control")(
        span(className := "button is-static is-small")("Show"),
      ),
      div(className := "control")(
        div(className := "select is-small")(
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
        ),
      ),
      div(className := "control")(
        span(className := "button is-static is-small")("entries"),
      ),
    )
  }

  override def renderInfo[T](state: TableState[T]): Html[TableUpdate] = {
    val start    = if (state.totalFilteredItems == 0) 0 else state.page.offset + 1
    val end      = math.min(state.page.offset + state.page.pageSize, state.totalFilteredItems)
    val total    = state.totalFilteredItems
    val allTotal = state.data.size

    p(className := "is-size-7")(
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
        val textValue   = currentState match {
          case FilterState.TextValue(v) => v
          case _                        => ""
        }
        input(
          className   := "input is-small",
          `type`      := "text",
          value       := textValue,
          placeholder := s"Search...",
          onInput(v => TableUpdate.SetFilter(column.id, FilterState.TextValue(v))),
        )

      case FilterType.Select =>
        val selectedValue = currentState match {
          case FilterState.SelectValue(v) => v.getOrElse("")
          case _                          => ""
        }
        val options       = state.uniqueValuesFor(column.id)
        div(className := "select is-small")(
          Html.select(
            onChange(v =>
              if (v.isEmpty) TableUpdate.ClearFilter(column.id)
              else TableUpdate.SetFilter(column.id, FilterState.SelectValue(Some(v))),
            ),
          )(
            option(value := "")("All") ::
              options.map(opt => option(value := opt, selected(selectedValue == opt))(opt)),
          ),
        )

      case FilterType.MultiSelect =>
        val selectedValues = currentState match {
          case FilterState.MultiSelectValue(v) => v
          case _                               => Set.empty[String]
        }
        val options        = state.uniqueValuesFor(column.id)
        div(className := "tags")(
          options.map { opt =>
            val isSelected = selectedValues.contains(opt)
            span(
              className := s"tag${if (isSelected) " is-primary" else " is-light"}",
              styles("cursor" -> "pointer"),
              onClick(
                if (isSelected)
                  TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues - opt))
                else
                  TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues + opt)),
              ),
            )(opt)
          },
        )

      case FilterType.NumberRange =>
        val (minVal, maxVal) = currentState match {
          case FilterState.NumberRangeValue(min, max) => (min, max)
          case _                                      => (None, None)
        }
        div(className := "field has-addons")(
          div(className := "control")(
            input(
              className   := "input is-small",
              `type`      := "number",
              placeholder := "Min",
              styles("width" -> "80px"),
              value       := minVal.map(_.toString).getOrElse(""),
              onInput(v =>
                TableUpdate.SetFilter(
                  column.id,
                  FilterState.NumberRangeValue(v.toDoubleOption, maxVal),
                ),
              ),
            ),
          ),
          div(className := "control")(
            span(className := "button is-static is-small")("-"),
          ),
          div(className := "control")(
            input(
              className   := "input is-small",
              `type`      := "number",
              placeholder := "Max",
              styles("width" -> "80px"),
              value       := maxVal.map(_.toString).getOrElse(""),
              onInput(v =>
                TableUpdate.SetFilter(
                  column.id,
                  FilterState.NumberRangeValue(minVal, v.toDoubleOption),
                ),
              ),
            ),
          ),
        )

      case FilterType.DateRange =>
        val (fromVal, toVal) = currentState match {
          case FilterState.DateRangeValue(from, to) => (from, to)
          case _                                    => (None, None)
        }
        div(className := "field has-addons")(
          div(className := "control")(
            input(
              className := "input is-small",
              `type`    := "date",
              value     := fromVal.map(_.toString).getOrElse(""),
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
          ),
          div(className := "control")(
            span(className := "button is-static is-small")("to"),
          ),
          div(className := "control")(
            input(
              className := "input is-small",
              `type`    := "date",
              value     := toVal.map(_.toString).getOrElse(""),
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
          ),
        )

      case FilterType.Boolean =>
        val boolValue = currentState match {
          case FilterState.BooleanValue(v) => v
          case _                           => None
        }
        div(className := "select is-small")(
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
          ),
        )
    }
  }
}
