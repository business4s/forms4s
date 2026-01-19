package forms4s.example.docs

import forms4s.datatable.*
import forms4s.datatable.derivation.*

import java.time.LocalDate

// start_model
case class Employee(
    name: String,
    department: String,
    salary: Double,
    hireDate: LocalDate,
    active: Boolean
)
// end_model

// start_table_def
val tableDef: TableDef[Employee] = TableDefBuilder[Employee]
  .modify(_.name)(_.withFilter(ColumnFilter.text))
  .modify(_.department)(_.withFilter(ColumnFilter.select))
  .modify(_.salary)(
    _.withFilter(ColumnFilter.numberRange(s => Some(s)))
      .withRender(s => f"$$$s%,.0f")
      .withSort(Ordering.Double.TotalOrdering)
  )
  .modify(_.hireDate)(_.withFilter(ColumnFilter.dateRange(d => Some(d))))
  .modify(_.active)(
    _.withFilter(ColumnFilter.boolean(identity))
      .withRender(b => if (b) "Yes" else "No")
  )
  .rename(_.hireDate, "Hire Date")
  .build("employees")
  .withPageSize(10)
  .withSelection(multi = true)
// end_table_def

// start_state
val employees: Vector[Employee] = Vector(
  Employee("Alice", "Engineering", 95000, LocalDate.of(2020, 3, 15), true),
  Employee("Bob", "Marketing", 75000, LocalDate.of(2019, 7, 22), true),
  // ...
)

val tableState: TableState[Employee] = TableState(tableDef, employees)
// end_state

// start_export
val csv: String = TableExport.toCSV(tableState)
// end_export
