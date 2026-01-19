package forms4s.tyrian.datatable

import forms4s.datatable.*
import tyrian.Html
import tyrian.Html.*

/**
 * Bootstrap 5 CSS framework styled table renderer.
 */
object BootstrapTableRenderer extends TableRenderer {

  override def renderTable[T](state: TableState[T]): Html[TableUpdate] = {
    div(className := "card")(
      div(className := "card-body")(
        div(className := "row mb-3")(
          div(className := "col-auto")(renderInfo(state)),
          div(className := "col-auto ms-auto")(renderPageSizeSelect(state)),
          div(className := "col-auto")(
            button(
              className := "btn btn-primary btn-sm",
              onClick(TableUpdate.ExportCSV)
            )("Export CSV")
          )
        ),
        renderFilters(state),
        div(className := "table-responsive")(
          Html.table(className := "table table-striped table-hover")(
            renderHeader(state),
            renderBody(state)
          )
        ),
        renderPagination(state)
      )
    )
  }

  override def renderFilters[T](state: TableState[T]): Html[TableUpdate] = {
    val filterColumns = state.definition.columns.filter(_.filter.isDefined)
    if (filterColumns.isEmpty) div()()
    else
      div(className := "row g-3 mb-3")(
        filterColumns.map { col =>
          col.filter match {
            case Some(filter) =>
              val currentState = state.filters.getOrElse(col.id, filter.emptyState)
              div(className := "col-auto")(
                label(className := "form-label small")(col.label),
                renderFilterInput(state, col.asInstanceOf[Column[T, Any]], filter.asInstanceOf[ColumnFilter[Any]], currentState)
              )
            case None => div()()
          }
        }
      )
  }

  override def renderHeader[T](state: TableState[T]): Html[TableUpdate] = {
    Html.thead(className := "table-light")(
      tr(
        state.definition.columns.map { col =>
          val sortIndicator = state.sort match {
            case Some(SortState(colId, dir)) if colId == col.id =>
              if (dir == SortDirection.Asc) " ↑" else " ↓"
            case _ => ""
          }

          if (col.sortable)
            th(
              attribute("scope", "col"),
              onClick(TableUpdate.ToggleSort(col.id)),
              styles("cursor" -> "pointer", "user-select" -> "none")
            )(col.label + sortIndicator)
          else
            th(attribute("scope", "col"))(col.label)
        }
      )
    )
  }

  override def renderBody[T](state: TableState[T]): Html[TableUpdate] = {
    Html.tbody(
      if (state.displayData.isEmpty)
        List(tr(
          td(attribute("colspan", state.definition.columns.size.toString), className := "text-center text-muted")(
            "No data available"
          )
        ))
      else
        state.displayData.zipWithIndex.toList.map { case (row, idx) =>
          val globalIdx = state.page.offset + idx
          val isSelected = state.selection.contains(globalIdx)
          val rowAttrs = List(
            if (state.definition.selectable) Some(onClick(TableUpdate.ToggleRowSelection(globalIdx))) else None,
            if (isSelected) Some(className := "table-active") else None
          ).flatten
          tr(rowAttrs*)(
            state.definition.columns.map { col =>
              val value = col.extract(row)
              td(col.render(value))
            }
          )
        }
    )
  }

  override def renderPagination[T](state: TableState[T]): Html[TableUpdate] = {
    val totalPages = state.totalPages
    val currentPage = state.page.currentPage

    Html.nav()(
      Html.ul(className := "pagination justify-content-center")(
        Html.li(className := s"page-item${if (currentPage == 0) " disabled" else ""}")(
          a(
            className := "page-link",
            onClick(TableUpdate.PrevPage),
            attribute("href", "#")
          )("Previous")
        ) ::
          renderPaginationNumbers(currentPage, totalPages) :::
          List(
            Html.li(className := s"page-item${if (currentPage >= totalPages - 1) " disabled" else ""}")(
              a(
                className := "page-link",
                onClick(TableUpdate.NextPage),
                attribute("href", "#")
              )("Next")
            )
          )
      )
    )
  }

  private def renderPaginationNumbers(current: Int, total: Int): List[Html[TableUpdate]] = {
    def pageItem(n: Int): Html[TableUpdate] =
      Html.li(className := s"page-item${if (n == current) " active" else ""}")(
        a(
          className := "page-link",
          onClick(TableUpdate.SetPage(n)),
          attribute("href", "#")
        )((n + 1).toString)
      )

    def ellipsis: Html[TableUpdate] =
      Html.li(className := "page-item disabled")(
        span(className := "page-link")("...")
      )

    if (total <= 7) {
      (0 until total).map(pageItem).toList
    } else {
      val pages = scala.collection.mutable.ListBuffer[Html[TableUpdate]]()
      pages += pageItem(0)

      if (current > 2) pages += ellipsis

      val start = math.max(1, current - 1)
      val end = math.min(total - 2, current + 1)
      (start to end).foreach(n => pages += pageItem(n))

      if (current < total - 3) pages += ellipsis

      if (total > 1) pages += pageItem(total - 1)

      pages.toList
    }
  }

  private def renderPageSizeSelect[T](state: TableState[T]): Html[TableUpdate] = {
    div(className := "input-group input-group-sm")(
      span(className := "input-group-text")("Show"),
      Html.select(
        className := "form-select form-select-sm",
        onChange(v => TableUpdate.SetPageSize(v.toInt))
      )(
        state.definition.pageSizeOptions.map { size =>
          option(
            value := size.toString,
            selected(state.page.pageSize == size)
          )(size.toString)
        }
      ),
      span(className := "input-group-text")("entries")
    )
  }

  override def renderInfo[T](state: TableState[T]): Html[TableUpdate] = {
    val start = if (state.totalFilteredItems == 0) 0 else state.page.offset + 1
    val end = math.min(state.page.offset + state.page.pageSize, state.totalFilteredItems)
    val total = state.totalFilteredItems
    val allTotal = state.data.size

    p(className := "text-muted small mb-0")(
      if (total == allTotal)
        s"Showing $start to $end of $total entries"
      else
        s"Showing $start to $end of $total entries (filtered from $allTotal total)"
    )
  }

  override def renderFilterInput[T, V](
      state: TableState[T],
      column: Column[T, V],
      filter: ColumnFilter[V],
      currentState: FilterState
  ): Html[TableUpdate] = {
    filter.filterType match {
      case FilterType.Text =>
        val textValue = currentState match {
          case FilterState.TextValue(v) => v
          case _                        => ""
        }
        input(
          className := "form-control form-control-sm",
          `type` := "text",
          value := textValue,
          placeholder := "Search...",
          onInput(v => TableUpdate.SetFilter(column.id, FilterState.TextValue(v)))
        )

      case FilterType.Select =>
        val selectedValue = currentState match {
          case FilterState.SelectValue(v) => v.getOrElse("")
          case _                          => ""
        }
        val options = state.uniqueValuesFor(column.id)
        Html.select(
          className := "form-select form-select-sm",
          onChange(v =>
            if (v.isEmpty) TableUpdate.ClearFilter(column.id)
            else TableUpdate.SetFilter(column.id, FilterState.SelectValue(Some(v)))
          )
        )(
          option(value := "")("All") ::
            options.map(opt => option(value := opt, selected(selectedValue == opt))(opt))
        )

      case FilterType.MultiSelect =>
        val selectedValues = currentState match {
          case FilterState.MultiSelectValue(v) => v
          case _                               => Set.empty[String]
        }
        val options = state.uniqueValuesFor(column.id)
        div(
          options.map { opt =>
            div(className := "form-check form-check-inline")(
              input(
                className := "form-check-input",
                `type` := "checkbox",
                id := s"${column.id}-$opt",
                checked(selectedValues.contains(opt)),
                onChange(_ =>
                  if (selectedValues.contains(opt))
                    TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues - opt))
                  else
                    TableUpdate.SetFilter(column.id, FilterState.MultiSelectValue(selectedValues + opt))
                )
              ),
              label(className := "form-check-label", htmlFor := s"${column.id}-$opt")(opt)
            )
          }
        )

      case FilterType.NumberRange =>
        val (minVal, maxVal) = currentState match {
          case FilterState.NumberRangeValue(min, max) => (min, max)
          case _                                      => (None, None)
        }
        div(className := "input-group input-group-sm")(
          input(
            className := "form-control",
            `type` := "number",
            placeholder := "Min",
            value := minVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.NumberRangeValue(v.toDoubleOption, maxVal)
              )
            )
          ),
          span(className := "input-group-text")("-"),
          input(
            className := "form-control",
            `type` := "number",
            placeholder := "Max",
            value := maxVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.NumberRangeValue(minVal, v.toDoubleOption)
              )
            )
          )
        )

      case FilterType.DateRange =>
        val (fromVal, toVal) = currentState match {
          case FilterState.DateRangeValue(from, to) => (from, to)
          case _                                    => (None, None)
        }
        div(className := "input-group input-group-sm")(
          input(
            className := "form-control",
            `type` := "date",
            value := fromVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.DateRangeValue(
                  if (v.isEmpty) None else Some(java.time.LocalDate.parse(v)),
                  toVal
                )
              )
            )
          ),
          span(className := "input-group-text")("to"),
          input(
            className := "form-control",
            `type` := "date",
            value := toVal.map(_.toString).getOrElse(""),
            onInput(v =>
              TableUpdate.SetFilter(
                column.id,
                FilterState.DateRangeValue(
                  fromVal,
                  if (v.isEmpty) None else Some(java.time.LocalDate.parse(v))
                )
              )
            )
          )
        )

      case FilterType.Boolean =>
        val boolValue = currentState match {
          case FilterState.BooleanValue(v) => v
          case _                           => None
        }
        Html.select(
          className := "form-select form-select-sm",
          onChange(v =>
            v match {
              case ""      => TableUpdate.ClearFilter(column.id)
              case "true"  => TableUpdate.SetFilter(column.id, FilterState.BooleanValue(Some(true)))
              case "false" => TableUpdate.SetFilter(column.id, FilterState.BooleanValue(Some(false)))
              case _       => TableUpdate.ClearFilter(column.id)
            }
          )
        )(
          option(value := "", selected(boolValue.isEmpty))("All"),
          option(value := "true", selected(boolValue.contains(true)))("Yes"),
          option(value := "false", selected(boolValue.contains(false)))("No")
        )
    }
  }
}
