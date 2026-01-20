package forms4s.datatable

import org.scalatest.freespec.AnyFreeSpec

class TableStateQueryParamsSpec extends AnyFreeSpec {

  case class Person(name: String, age: Int, department: String, active: Boolean, tags: Set[String])

  val testData: Vector[Person] = Vector(
    Person("Alice", 30, "Engineering", true, Set("dev", "lead")),
    Person("Bob", 25, "Marketing", false, Set("marketing")),
    Person("Carol", 35, "Engineering", true, Set("dev")),
    Person("David", 28, "Sales", true, Set("sales", "lead")),
    Person("Eve", 32, "Marketing", false, Set("marketing", "design")),
  )

  val tableDef: TableDef[Person] = TableDef(
    id = "people",
    columns = List(
      Column[Person, String]("name", "Name", _.name).withFilter(ColumnFilter.text),
      Column[Person, Int]("age", "Age", _.age, _.toString).withFilter(ColumnFilter.numberRange(a => Some(a.toDouble))),
      Column[Person, String]("department", "Department", _.department).withFilter(ColumnFilter.select),
      Column[Person, Boolean]("active", "Active", _.active, b => if (b) "Yes" else "No")
        .withFilter(ColumnFilter.boolean(identity)),
      Column[Person, Set[String]]("tags", "Tags", _.tags, _.mkString(", ")).withFilter(ColumnFilter.multiSelect(_.mkString(", "))),
    ),
    pageSize = 2, // Small page size to allow pagination tests
  )

  def initialState: TableState[Person] = TableState(tableDef, testData)

  "TableStateQueryParams" - {
    "toQueryString" - {
      "empty state produces empty string" in {
        assert(initialState.toQueryString == "")
      }

      "serializes sort" in {
        val state = initialState.update(TableUpdate.SetSort("name", SortDirection.Asc))
        assert(state.toQueryString == "sort=name%3Aasc")
      }

      "serializes sort descending" in {
        val state = initialState.update(TableUpdate.SetSort("age", SortDirection.Desc))
        assert(state.toQueryString == "sort=age%3Adesc")
      }

      "serializes page when not 0" in {
        val state = initialState.update(TableUpdate.SetPage(2))
        assert(state.toQueryString == "page=2")
      }

      "does not serialize page when 0" in {
        val state = initialState.update(TableUpdate.SetPage(0))
        assert(state.toQueryString == "")
      }

      "serializes page size when different from default" in {
        val state = initialState.update(TableUpdate.SetPageSize(25))
        assert(state.toQueryString == "size=25")
      }

      "does not serialize page size when same as default" in {
        val state = initialState.update(TableUpdate.SetPageSize(2))
        assert(state.toQueryString == "")
      }

      "serializes text filter" in {
        val state = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("alice")))
        assert(state.toQueryString == "f.name=alice")
      }

      "serializes text filter with special characters" in {
        val state = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("John Doe")))
        assert(state.toQueryString == "f.name=John+Doe")
      }

      "serializes select filter" in {
        val state = initialState.update(TableUpdate.SetFilter("department", FilterState.SelectValue(Some("Engineering"))))
        assert(state.toQueryString == "f.department=Engineering")
      }

      "serializes multi-select filter" in {
        val state =
          initialState.update(TableUpdate.SetFilter("department", FilterState.MultiSelectValue(Set("Engineering", "Marketing"))))
        val qs    = state.toQueryString
        assert(qs.contains("f.department=Engineering"))
        assert(qs.contains("f.department=Marketing"))
      }

      "serializes boolean filter true" in {
        val state = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        assert(state.toQueryString == "f.active=true")
      }

      "serializes boolean filter false" in {
        val state = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(false))))
        assert(state.toQueryString == "f.active=false")
      }

      "serializes number range filter - min only" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(25), None)))
        assert(state.toQueryString == "f.age.min=25")
      }

      "serializes number range filter - max only" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(None, Some(30))))
        assert(state.toQueryString == "f.age.max=30")
      }

      "serializes number range filter - both" in {
        val state = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(25), Some(30))))
        val qs    = state.toQueryString
        assert(qs.contains("f.age.min=25"))
        assert(qs.contains("f.age.max=30"))
      }

      "serializes complex state" in {
        val state = initialState
          .update(TableUpdate.SetSort("name", SortDirection.Desc))
          .update(TableUpdate.SetPageSize(3)) // 5 items / 3 = 2 pages
          .update(TableUpdate.SetPage(1))
        val qs = state.toQueryString
        assert(qs.contains("sort=name%3Adesc"))
        assert(qs.contains("page=1"))
        assert(qs.contains("size=3"))
      }

      "serializes filters combined with other state" in {
        val state = initialState
          .update(TableUpdate.SetSort("name", SortDirection.Desc))
          .update(TableUpdate.SetFilter("name", FilterState.TextValue("a")))
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        val qs    = state.toQueryString
        assert(qs.contains("sort=name%3Adesc"))
        assert(qs.contains("f.name=a"))
        assert(qs.contains("f.active=true"))
      }
    }

    "fromQueryString" - {
      "empty string produces empty params" in {
        val params = TableStateQueryParams.fromQueryString("")
        assert(params == TableStateParams())
      }

      "handles leading question mark" in {
        val params = TableStateQueryParams.fromQueryString("?page=1")
        assert(params.page == Some(1))
      }

      "parses sort" in {
        val params = TableStateQueryParams.fromQueryString("sort=name:asc")
        assert(params.sort == Some(SortState("name", SortDirection.Asc)))
      }

      "parses sort descending" in {
        val params = TableStateQueryParams.fromQueryString("sort=age:desc")
        assert(params.sort == Some(SortState("age", SortDirection.Desc)))
      }

      "parses page" in {
        val params = TableStateQueryParams.fromQueryString("page=2")
        assert(params.page == Some(2))
      }

      "parses page size" in {
        val params = TableStateQueryParams.fromQueryString("size=25")
        assert(params.pageSize == Some(25))
      }

      "parses simple filter value" in {
        val params = TableStateQueryParams.fromQueryString("f.name=alice")
        assert(params.filters("name") == ParsedFilterValue.SimpleValues(Seq("alice")))
      }

      "parses multi-value filter from repeated params" in {
        val params = TableStateQueryParams.fromQueryString("f.department=Engineering&f.department=Marketing")
        assert(params.filters("department") == ParsedFilterValue.SimpleValues(Seq("Engineering", "Marketing")))
      }

      "parses range filter" in {
        val params = TableStateQueryParams.fromQueryString("f.age.min=25&f.age.max=30")
        assert(params.filters("age") == ParsedFilterValue.RangeValue(Some("25"), Some("30")))
      }

      "handles URL-encoded values" in {
        val params = TableStateQueryParams.fromQueryString("f.name=John%20Doe")
        assert(params.filters("name") == ParsedFilterValue.SimpleValues(Seq("John Doe")))
      }

      "handles invalid sort gracefully" in {
        val params = TableStateQueryParams.fromQueryString("sort=invalid")
        assert(params.sort.isEmpty)
      }

      "handles invalid page gracefully" in {
        val params = TableStateQueryParams.fromQueryString("page=abc")
        assert(params.page.isEmpty)
      }

      "handles negative page gracefully" in {
        val params = TableStateQueryParams.fromQueryString("page=-1")
        assert(params.page.isEmpty)
      }
    }

    "roundtrip" - {
      "text filter roundtrip" in {
        val state    = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("alice")))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("name") == FilterState.TextValue("alice"))
      }

      "text filter with special characters roundtrip" in {
        val state    = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("O'Brien & Co.")))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("name") == FilterState.TextValue("O'Brien & Co."))
      }

      "boolean filter roundtrip" in {
        val state    = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("active") == FilterState.BooleanValue(Some(true)))
      }

      "number range filter roundtrip" in {
        val state    = initialState.update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(25), Some(30))))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("age") == FilterState.NumberRangeValue(Some(25.0), Some(30.0)))
      }

      "sort roundtrip" in {
        val state    = initialState.update(TableUpdate.SetSort("name", SortDirection.Desc))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.sort == Some(SortState("name", SortDirection.Desc)))
      }

      "pagination roundtrip" in {
        // Default page size is 2, so with 5 items we have 3 pages (0, 1, 2)
        val state    = initialState.update(TableUpdate.SetPage(2))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.page.currentPage == 2)
        assert(newState.page.pageSize == 2)
      }

      "complex state roundtrip" in {
        val state = initialState
          .update(TableUpdate.SetSort("name", SortDirection.Desc))
          .update(TableUpdate.SetPageSize(3)) // 5 items / 3 = 2 pages
          .update(TableUpdate.SetPage(1))
          .update(TableUpdate.SetFilter("name", FilterState.TextValue("a")))
          .update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
          .update(TableUpdate.SetFilter("age", FilterState.NumberRangeValue(Some(20), Some(40))))

        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)

        assert(newState.sort == state.sort)
        assert(newState.page.currentPage == state.page.currentPage)
        assert(newState.page.pageSize == state.page.pageSize)
        assert(newState.filters("name") == state.filters("name"))
        assert(newState.filters("active") == state.filters("active"))
        assert(newState.filters("age") == FilterState.NumberRangeValue(Some(20.0), Some(40.0)))
      }
    }

    "toQueryParams and fromQueryParams" - {
      "multi-select filter with toQueryParams" in {
        val state      = initialState.update(TableUpdate.SetFilter("department", FilterState.MultiSelectValue(Set("A", "B", "C"))))
        val params     = state.toQueryParams
        val deptParams = params.filter(_._1 == "f.department").map(_._2).toSet
        assert(deptParams == Set("A", "B", "C"))
      }

      "fromQueryParams handles repeated keys" in {
        val params = Seq(
          "f.department" -> "Engineering",
          "f.department" -> "Marketing",
          "f.department" -> "Sales",
        )
        val parsed = TableStateQueryParams.fromQueryParams(params)
        assert(parsed.filters("department") == ParsedFilterValue.SimpleValues(Seq("Engineering", "Marketing", "Sales")))
      }
    }

    "loadFromQueryParams extension" - {
      "applies params to state using column filter type" in {
        val params   = Seq("sort" -> "name:asc", "page" -> "1", "f.active" -> "true")
        val newState = initialState.loadFromQueryParams(params)
        assert(newState.sort == Some(SortState("name", SortDirection.Asc)))
        assert(newState.page.currentPage == 1)
        // The BooleanValue is determined by the column's filter type in TableDef
        assert(newState.filters("active") == FilterState.BooleanValue(Some(true)))
      }
    }

    "filter type inference from TableDef" - {
      "text filter roundtrip preserves type" in {
        val state    = initialState.update(TableUpdate.SetFilter("name", FilterState.TextValue("alice")))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("name") == FilterState.TextValue("alice"))
      }

      "select filter roundtrip preserves type" in {
        val state    = initialState.update(TableUpdate.SetFilter("department", FilterState.SelectValue(Some("Engineering"))))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("department") == FilterState.SelectValue(Some("Engineering")))
      }

      "multi-select filter roundtrip preserves type" in {
        val state    = initialState.update(TableUpdate.SetFilter("tags", FilterState.MultiSelectValue(Set("dev", "lead"))))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("tags") == FilterState.MultiSelectValue(Set("dev", "lead")))
      }

      "boolean filter roundtrip preserves type" in {
        val state    = initialState.update(TableUpdate.SetFilter("active", FilterState.BooleanValue(Some(true))))
        val qs       = state.toQueryString
        val newState = initialState.loadFromQueryString(qs)
        assert(newState.filters("active") == FilterState.BooleanValue(Some(true)))
      }
    }
  }
}
