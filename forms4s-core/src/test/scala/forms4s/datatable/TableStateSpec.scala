package forms4s.datatable

import org.scalatest.freespec.AnyFreeSpec

class TableStateSpec extends AnyFreeSpec {

  case class Person(name: String, age: Int, active: Boolean)

  val testData: Vector[Person] = Vector(
    Person("Alice", 30, true),
    Person("Bob", 25, false),
    Person("Carol", 35, true),
    Person("David", 28, true),
    Person("Eve", 22, false),
  )

  val tableDef: TableDef[Person] = TableDef(
    id = "people",
    columns = List(
      Column[Person, String]("name", "Name", _.name).withFilter(ColumnFilter.text),
      Column[Person, Int]("age", "Age", _.age, _.toString).withFilter(ColumnFilter.numberRange(a => Some(a.toDouble))),
      Column[Person, Boolean]("active", "Active", _.active, b => if (b) "Yes" else "No").withFilter(ColumnFilter.boolean(identity)),
    ),
    pageSize = 2,
  )

  def initialState: TableState[Person] = TableState(tableDef, testData)

  "TableState" - {
    "filtering" - {
      "text filter matches case-insensitively" in {
        val state = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("ali")))
        assert(state.filteredData == Vector(Person("Alice", 30, true)))
      }

      "text filter with empty value returns all data" in {
        val state = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("")))
        assert(state.filteredData == testData)
      }

      "number range filter - min only" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(28), None)))
        assert(
          state.filteredData == Vector(
            Person("Alice", 30, true),
            Person("Carol", 35, true),
            Person("David", 28, true),
          ),
        )
      }

      "number range filter - max only" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(None, Some(25))))
        assert(
          state.filteredData == Vector(
            Person("Bob", 25, false),
            Person("Eve", 22, false),
          ),
        )
      }

      "number range filter - both min and max" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(25), Some(30))))
        assert(
          state.filteredData == Vector(
            Person("Alice", 30, true),
            Person("Bob", 25, false),
            Person("David", 28, true),
          ),
        )
      }

      "boolean filter - true" in {
        val state = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        assert(
          state.filteredData == Vector(
            Person("Alice", 30, true),
            Person("Carol", 35, true),
            Person("David", 28, true),
          ),
        )
      }

      "boolean filter - false" in {
        val state = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(false))))
        assert(
          state.filteredData == Vector(
            Person("Bob", 25, false),
            Person("Eve", 22, false),
          ),
        )
      }

      "multiple filters combine with AND" in {
        val state = initialState
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
          .update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(30), None)))
        assert(
          state.filteredData == Vector(
            Person("Alice", 30, true),
            Person("Carol", 35, true),
          ),
        )
      }

      "clear filter removes it" in {
        val state = initialState
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
          .update(TableUpdate.ClearFilter("active"))
        assert(state.filteredData == testData)
      }

      "clear all filters" in {
        val state = initialState
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
          .update(TableUpdate.SetFilter("name", FilterState.TextValue("a")))
          .update(TableUpdate.ClearAllFilters)
        assert(state.filteredData == testData)
      }
    }

    "sorting" - {
      "sort ascending by string" in {
        val state = initialState.update(TableUpdate.SetSort("name", SortDirection.Asc))
        assert(state.sortedData.map(_.name) == Vector("Alice", "Bob", "Carol", "David", "Eve"))
      }

      "sort descending by string" in {
        val state = initialState.update(TableUpdate.SetSort("name", SortDirection.Desc))
        assert(state.sortedData.map(_.name) == Vector("Eve", "David", "Carol", "Bob", "Alice"))
      }

      "toggle sort flips direction" in {
        val state1 = initialState.update(TableUpdate.ToggleSort("name"))
        assert(state1.sort == Some(SortState("name", SortDirection.Asc)))

        val state2 = state1.update(TableUpdate.ToggleSort("name"))
        assert(state2.sort == Some(SortState("name", SortDirection.Desc)))
      }

      "toggle sort on different column resets to ascending" in {
        val state = initialState
          .update(TableUpdate.SetSort("name", SortDirection.Desc))
          .update(TableUpdate.ToggleSort("age"))
        assert(state.sort == Some(SortState("age", SortDirection.Asc)))
      }

      "clear sort removes sorting" in {
        val state = initialState
          .update(TableUpdate.SetSort("name", SortDirection.Asc))
          .update(TableUpdate.ClearSort)
        assert(state.sort.isEmpty)
      }
    }

    "pagination" - {
      "initial page is 0" in {
        assert(initialState.page.currentPage == 0)
      }

      "page size from definition" in {
        assert(initialState.page.pageSize == 2)
      }

      "paged data respects page size" in {
        assert(initialState.pagedData.size == 2)
        assert(initialState.pagedData == Vector(Person("Alice", 30, true), Person("Bob", 25, false)))
      }

      "next page advances" in {
        val state = initialState.update(TableUpdate.NextPage)
        assert(state.page.currentPage == 1)
        assert(state.pagedData == Vector(Person("Carol", 35, true), Person("David", 28, true)))
      }

      "prev page goes back" in {
        val state = initialState
          .update(TableUpdate.SetPage(2))
          .update(TableUpdate.PrevPage)
        assert(state.page.currentPage == 1)
      }

      "cannot go below page 0" in {
        val state = initialState.update(TableUpdate.PrevPage)
        assert(state.page.currentPage == 0)
      }

      "cannot exceed max page" in {
        val state = initialState.update(TableUpdate.SetPage(100))
        assert(state.page.currentPage == 2) // 5 items / 2 per page = 3 pages (0, 1, 2)
      }

      "first page goes to 0" in {
        val state = initialState
          .update(TableUpdate.SetPage(2))
          .update(TableUpdate.FirstPage)
        assert(state.page.currentPage == 0)
      }

      "last page goes to final page" in {
        val state = initialState.update(TableUpdate.LastPage)
        assert(state.page.currentPage == 2)
      }

      "change page size resets to page 0" in {
        val state = initialState
          .update(TableUpdate.SetPage(1))
          .update(TableUpdate.SetPageSize(10))
        assert(state.page.currentPage == 0)
        assert(state.page.pageSize == 10)
      }

      "filtering resets to page 0" in {
        val state = initialState
          .update(TableUpdate.SetPage(2))
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        assert(state.page.currentPage == 0)
      }
    }

    "selection" - {
      val selectableDef   = tableDef.withSelection(multi = true)
      def selectableState = TableState(selectableDef, testData)

      "select row" in {
        val state = selectableState.update(TableUpdate.SelectRow(0))
        assert(state.selection == Set(0))
      }

      "multi-select adds to selection" in {
        val state = selectableState
          .update(TableUpdate.SelectRow(0))
          .update(TableUpdate.SelectRow(2))
        assert(state.selection == Set(0, 2))
      }

      "toggle selection" in {
        val state1 = selectableState.update(TableUpdate.ToggleRowSelection(0))
        assert(state1.selection == Set(0))

        val state2 = state1.update(TableUpdate.ToggleRowSelection(0))
        assert(state2.selection == Set.empty)
      }

      "select all" in {
        val state = selectableState.update(TableUpdate.SelectAll)
        assert(state.selection == Set(0, 1, 2, 3, 4))
      }

      "deselect all" in {
        val state = selectableState
          .update(TableUpdate.SelectAll)
          .update(TableUpdate.DeselectAll)
        assert(state.selection == Set.empty)
      }

      "selected items returns correct data" in {
        val state = selectableState
          .update(TableUpdate.SelectRow(1))
          .update(TableUpdate.SelectRow(3))
        assert(state.selectedItems == Vector(Person("Bob", 25, false), Person("David", 28, true)))
      }

      "select all with filter uses original data indices" in {
        // Filter to only active=true: Alice(0), Carol(2), David(3)
        val state = selectableState
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
          .update(TableUpdate.SelectAll)
        // Should select original indices 0, 2, 3 - NOT 0, 1, 2
        assert(state.selection == Set(0, 2, 3))
        assert(state.selectedItems == Vector(Person("Alice", 30, true), Person("Carol", 35, true), Person("David", 28, true)))
      }

      "selectedItems returns correct data after filter and select all" in {
        // Filter to only active=false: Bob(1), Eve(4)
        val state = selectableState
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(false))))
          .update(TableUpdate.SelectAll)
        assert(state.selection == Set(1, 4))
        assert(state.selectedItems == Vector(Person("Bob", 25, false), Person("Eve", 22, false)))
      }

      "displayDataWithIndices preserves original indices through sort" in {
        // Sort by name descending: Eve(4), David(3), Carol(2), Bob(1), Alice(0)
        val state     = selectableState.update(TableUpdate.SetSort("name", SortDirection.Desc))
        val displayed = state.displayDataWithIndices
        // First page (size 2): Eve(4), David(3)
        assert(displayed == Vector((Person("Eve", 22, false), 4), (Person("David", 28, true), 3)))
      }

      "selection persists correctly after sorting" in {
        // Select Alice (original index 0), then sort descending
        val state = selectableState
          .update(TableUpdate.SelectRow(0))
          .update(TableUpdate.SetSort("name", SortDirection.Desc))
        // Alice is still selected by her original index 0
        assert(state.selection == Set(0))
        // Alice should still be in selectedItems
        assert(state.selectedItems == Vector(Person("Alice", 30, true)))
        // displayDataWithIndices on page 0 shows Eve(4), David(3) - neither selected
        assert(state.displayDataWithIndices.map(_._2) == Vector(4, 3))
      }
    }

    "unique values" - {
      "returns distinct sorted values" in {
        val data  = Vector(
          Person("Alice", 30, true),
          Person("Bob", 30, false),
          Person("Carol", 25, true),
        )
        val state = TableState(tableDef, data)
        assert(state.uniqueValuesFor("age") == List("25", "30"))
      }
    }

    "server mode" - {
      def serverState = TableState.serverMode(tableDef)

      "creates state with good deafaults" in {
        assert(serverState.serverMode)
        assert(serverState.data.isEmpty)
        assert(serverState.loading == LoadingState.Idle)
      }

      "setLoading sets loading to Loading" in {
        val state = serverState.setLoading
        assert(state.loading == LoadingState.Loading)
      }

      "setError sets loading to Failed with message" in {
        val state = serverState.setError("Network error")
        assert(state.loading == LoadingState.Failed("Network error"))
      }

      "setServerData sets data, totalOverride, and resets loading" in {
        val serverData = Vector(Person("Test", 99, true))
        val state      = serverState.setLoading.setServerData(serverData, 100)

        assert(state.data == serverData)
        assert(state.totalOverride == Some(100))
        assert(state.loading == LoadingState.Idle)
        assert(state.selection.isEmpty)
      }

      "displayData returns data directly in server mode (no local filtering/paging)" in {
        val serverData = Vector(
          Person("Page1A", 1, true),
          Person("Page1B", 2, true),
        )
        val state      = serverState.setServerData(serverData, 50)

        // In server mode, displayData should return exactly what server sent
        assert(state.displayData == serverData)
      }

      "totalFilteredItems uses totalOverride in server mode" in {
        val serverData = Vector(Person("Test", 99, true))
        val state      = serverState.setServerData(serverData, 150)

        // data.size is 1, but totalOverride is 150
        assert(state.totalFilteredItems == 150)
      }

      "totalPages calculated from totalOverride" in {
        val serverData = Vector(Person("Test", 99, true))
        val state      = serverState.setServerData(serverData, 25) // 25 items, page size 2 = 13 pages

        assert(state.totalPages == 13)
      }

      "client mode still uses local data for totalFilteredItems" in {
        val state = initialState
        // No totalOverride, so uses filteredData.size
        assert(state.totalFilteredItems == 5)
        assert(state.totalOverride.isEmpty)
      }

      "update preserves serverMode flag" in {
        val state = serverState
          .update(TableUpdate.SetFilter("name", FilterState.TextValue("test")))
          .update(TableUpdate.ToggleSort("age"))
          .update(TableUpdate.SetPage(2))

        assert(state.serverMode)
      }

      "filter/sort/page state tracked for query params in server mode" in {
        val state = serverState
          .update(TableUpdate.SetFilter("name", FilterState.TextValue("alice")))
          .update(TableUpdate.SetSort("age", SortDirection.Desc))
          .update(TableUpdate.SetPageSize(10))

        assert(state.filters.contains("name"))
        assert(state.sort == Some(SortState("age", SortDirection.Desc)))
        assert(state.page.pageSize == 10)

        // Can generate query params to send to server
        val queryParams = state.toQueryParams
        assert(queryParams.contains(("f.name", "alice")))
        assert(queryParams.contains(("sort", "age:desc")))
        assert(queryParams.contains(("size", "10")))
      }

      "displayDataWithIndices returns data.zipWithIndex in server mode" in {
        val serverData = Vector(
          Person("A", 1, true),
          Person("B", 2, false),
        )
        val state      = serverState.setServerData(serverData, 10)

        // In server mode, indices are 0-based for current page (not original data indices)
        assert(state.displayDataWithIndices == Vector((Person("A", 1, true), 0), (Person("B", 2, false), 1)))
      }
    }
  }
}
