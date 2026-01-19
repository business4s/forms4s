package forms4s.datatable

import org.scalatest.freespec.AnyFreeSpec

class TableExportSpec extends AnyFreeSpec {

  case class Person(name: String, city: String, salary: Int)

  val testData: Vector[Person] = Vector(
    Person("Alice", "New York", 50000),
    Person("Bob", "Los Angeles", 60000),
    Person("Carol", "Chicago", 55000),
  )

  val tableDef: TableDef[Person] = TableDef(
    id = "people",
    columns = List(
      Column[Person, String]("name", "Name", _.name),
      Column[Person, String]("city", "City", _.city),
      Column[Person, Int]("salary", "Salary", _.salary, _.toString),
    ),
    pageSize = 10,
  )

  def state: TableState[Person] = TableState(tableDef, testData)

  "TableExport" - {
    "toCSV" - {
      "exports with default comma delimiter" in {
        val csv = TableExport.toCSV(state)
        val lines = csv.split("\n")
        assert(lines(0) == "Name,City,Salary")
        assert(lines(1) == "Alice,New York,50000")
        assert(lines(2) == "Bob,Los Angeles,60000")
        assert(lines(3) == "Carol,Chicago,55000")
      }

      "exports with custom semicolon delimiter" in {
        val csv = TableExport.toCSV(state, delimiter = ";")
        val lines = csv.split("\n")
        assert(lines(0) == "Name;City;Salary")
        assert(lines(1) == "Alice;New York;50000")
      }

      "exports with custom tab delimiter" in {
        val csv = TableExport.toCSV(state, delimiter = "\t")
        val lines = csv.split("\n")
        assert(lines(0) == "Name\tCity\tSalary")
        assert(lines(1) == "Alice\tNew York\t50000")
      }

      "escapes values containing the delimiter" in {
        val dataWithComma = Vector(Person("Alice, Jr.", "New York", 50000))
        val stateWithComma = TableState(tableDef, dataWithComma)
        val csv = TableExport.toCSV(stateWithComma)
        val lines = csv.split("\n")
        // Name contains comma, so it should be quoted
        assert(lines(1) == "\"Alice, Jr.\",New York,50000")
      }

      "escapes values containing semicolon when using semicolon delimiter" in {
        val dataWithSemi = Vector(Person("Alice; Bob", "New York", 50000))
        val stateWithSemi = TableState(tableDef, dataWithSemi)
        val csv = TableExport.toCSV(stateWithSemi, delimiter = ";")
        val lines = csv.split("\n")
        // Name contains semicolon, so it should be quoted
        assert(lines(1) == "\"Alice; Bob\";New York;50000")
      }

      "does not escape semicolon when using comma delimiter" in {
        val dataWithSemi = Vector(Person("Alice; Bob", "New York", 50000))
        val stateWithSemi = TableState(tableDef, dataWithSemi)
        val csv = TableExport.toCSV(stateWithSemi, delimiter = ",")
        val lines = csv.split("\n")
        // Name contains semicolon but delimiter is comma, so no quoting needed
        assert(lines(1) == "Alice; Bob,New York,50000")
      }

      "escapes quotes in values" in {
        val dataWithQuote = Vector(Person("Alice \"The Great\"", "New York", 50000))
        val stateWithQuote = TableState(tableDef, dataWithQuote)
        val csv = TableExport.toCSV(stateWithQuote)
        val lines = csv.split("\n")
        // Quotes should be doubled and value should be quoted
        assert(lines(1) == "\"Alice \"\"The Great\"\"\",New York,50000")
      }

      "escapes newlines in values" in {
        val dataWithNewline = Vector(Person("Alice\nBob", "New York", 50000))
        val stateWithNewline = TableState(tableDef, dataWithNewline)
        val csv = TableExport.toCSV(stateWithNewline)
        val lines = csv.split("\n")
        // Value with newline should be quoted
        assert(lines(1).startsWith("\"Alice"))
      }

      "can exclude headers" in {
        val csv = TableExport.toCSV(state, includeHeaders = false)
        val lines = csv.split("\n")
        assert(lines(0) == "Alice,New York,50000")
      }
    }
  }
}
