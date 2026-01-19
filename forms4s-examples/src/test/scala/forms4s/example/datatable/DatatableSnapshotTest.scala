package forms4s.example.datatable

import forms4s.datatable.*
import forms4s.datatable.derivation.*
import forms4s.testing.SnapshotTest
import org.scalatest.funsuite.AnyFunSuite

import java.time.LocalDate

class DatatableSnapshotTest extends AnyFunSuite {

  // Test data model
  case class Employee(
      id: Int,
      name: String,
      department: String,
      salary: Double,
      hireDate: LocalDate,
      active: Boolean,
  )

  val testEmployees: Vector[Employee] = Vector(
    Employee(1, "Alice Smith", "Engineering", 95000, LocalDate.of(2020, 3, 15), true),
    Employee(2, "Bob Johnson", "Marketing", 75000, LocalDate.of(2019, 7, 22), true),
    Employee(3, "Carol Williams", "Engineering", 105000, LocalDate.of(2018, 1, 10), true),
    Employee(4, "David Brown", "Sales", 65000, LocalDate.of(2021, 11, 5), false),
    Employee(5, "Eve Martinez", "HR", 70000, LocalDate.of(2020, 6, 1), true),
  )

  // Table definition with customizations
  val tableDef: TableDef[Employee] = TableDefBuilder[Employee]
    .exclude(_.id)
    .modify(_.salary)(_.withRender(s => f"$$$s%,.0f"))
    .modify(_.active)(_.withRender(b => if (b) "Yes" else "No"))
    .rename(_.hireDate, "Hire Date")
    .build("employees")
    .withPageSize(10)

  test("CSV export - all data") {
    val state = TableState(tableDef, testEmployees)
    val csv   = TableExport.toCSV(state)
    SnapshotTest.testSnapshot(csv, "datatable/export-all.csv")
  }

  test("CSV export - filtered data") {
    val state = TableState(tableDef, testEmployees)
      .update(TableUpdate.SetFilter("department", FilterState.SelectValue(Some("Engineering"))))
    val csv   = TableExport.toCSV(state)
    SnapshotTest.testSnapshot(csv, "datatable/export-filtered.csv")
  }

  test("CSV export - sorted data") {
    val state = TableState(tableDef, testEmployees)
      .update(TableUpdate.SetSort("salary", SortDirection.Desc))
    val csv   = TableExport.toCSV(state)
    SnapshotTest.testSnapshot(csv, "datatable/export-sorted.csv")
  }

  test("CSV export - selected rows only") {
    val selectableDef = tableDef.withSelection(multi = true)
    val state         = TableState(selectableDef, testEmployees)
      .update(TableUpdate.SelectRow(0))
      .update(TableUpdate.SelectRow(2))
    val csv           = TableExport.selectedToCSV(state)
    SnapshotTest.testSnapshot(csv, "datatable/export-selected.csv")
  }

  test("CSV export - with special characters (escaping)") {
    case class Record(name: String, description: String)
    val recordDef = TableDef[Record](
      "records",
      List(
        Column[Record, String]("name", "Name", _.name),
        Column[Record, String]("description", "Description", _.description),
      ),
    )
    val data      = Vector(
      Record("Normal", "Just text"),
      Record("With Comma", "Hello, World"),
      Record("With Quote", """He said "Hello""""),
      Record("With Newline", "Line1\nLine2"),
      Record("All Combined", """Comma, Quote", Newline\n"""),
    )
    val state     = TableState(recordDef, data)
    val csv       = TableExport.toCSV(state)
    SnapshotTest.testSnapshot(csv, "datatable/export-escaping.csv")
  }

  test("derived columns from case class") {
    // This tests that TableDefBuilder correctly derives columns from a case class
    val columns    = tableDef.columns
    val columnInfo = columns
      .map { col =>
        s"${col.id}: ${col.label}"
      }
      .mkString("\n")
    SnapshotTest.testSnapshot(columnInfo, "datatable/derived-columns.txt")
  }

  test("column extraction works correctly") {
    // Verify that the derived extractors work
    val employee = testEmployees.head
    val values   = tableDef.columns
      .map { col =>
        s"${col.id}=${col.render(col.extract(employee))}"
      }
      .mkString("\n")
    SnapshotTest.testSnapshot(values, "datatable/column-extraction.txt")
  }
}
