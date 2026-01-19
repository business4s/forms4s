package forms4s.example.components

import cats.effect.IO
import forms4s.datatable.*
import forms4s.datatable.derivation.*
import forms4s.tyrian.datatable.*
import tyrian.*
import tyrian.Html.*
import org.scalajs.dom

import java.time.LocalDate
import scala.scalajs.js

// Data model for the example
case class Employee(
    id: Int,
    name: String,
    email: String,
    department: String,
    salary: Double,
    hireDate: LocalDate,
    active: Boolean
)

enum DatatableMsg {
  case TableMsg(msg: TableUpdate)
  case FrameworkSelected(framework: CssFramework)
}

case class DatatablePlayground(
    tableState: TableState[Employee],
    framework: CssFramework
) {

  private val renderer: TableRenderer = framework match {
    case CssFramework.Bulma     => BulmaTableRenderer
    case CssFramework.Bootstrap => BootstrapTableRenderer
    case CssFramework.Raw       => RawTableRenderer
    case CssFramework.Pico      => RawTableRenderer
  }

  private val rendererLabel: String = framework match {
    case CssFramework.Bulma     => "bulma"
    case CssFramework.Bootstrap => "bootstrap"
    case CssFramework.Raw       => "raw"
    case CssFramework.Pico      => "picocss"
  }

  def update: DatatableMsg => (DatatablePlayground, Cmd[IO, DatatableMsg]) = {
    case DatatableMsg.TableMsg(TableUpdate.ExportCSV) =>
      val csv = TableExport.toCSV(tableState)
      // Trigger browser download
      val blob = new dom.Blob(
        js.Array(csv),
        new dom.BlobPropertyBag { `type` = "text/csv;charset=utf-8" }
      )
      val url = dom.URL.createObjectURL(blob)
      val link = dom.document.createElement("a").asInstanceOf[dom.HTMLAnchorElement]
      link.href = url
      link.setAttribute("download", "export.csv")
      link.click()
      dom.URL.revokeObjectURL(url)
      (this, Cmd.None)

    case DatatableMsg.TableMsg(msg) =>
      (copy(tableState = tableState.update(msg)), Cmd.None)

    case DatatableMsg.FrameworkSelected(newFramework) =>
      (copy(framework = newFramework), Cmd.None)
  }

  def render: Html[DatatableMsg] = {
    div(className := "container")(
      div(className := "notification is-info is-light")(
        "Interactive datatable with filtering, sorting, and pagination. Try clicking column headers to sort, or use the filters."
      ),
      div(className := "columns")(
        div(className := "column is-full")(
          section(className := "box")(
            div(className := "level")(
              div(className := "level-left")(
                div(className := "level-item")(
                  h2(className := "title is-5")("Employee Directory")
                )
              ),
              div(className := "level-right")(
                div(className := "level-item")(
                  Html.div(`class` := "select is-small")(
                    Html.select(
                      id := "css-framework-datatable",
                      name := "css-framework-datatable",
                      onChange(value => DatatableMsg.FrameworkSelected(CssFramework.valueOf(value)))
                    )(
                      Html.option(value := CssFramework.Raw.toString, selected := (framework == CssFramework.Raw))("Raw"),
                      Html.option(value := CssFramework.Pico.toString, selected := (framework == CssFramework.Pico))("Pico"),
                      Html.option(value := CssFramework.Bulma.toString, selected := (framework == CssFramework.Bulma))("Bulma"),
                      Html.option(
                        value := CssFramework.Bootstrap.toString,
                        selected := (framework == CssFramework.Bootstrap)
                      )("Bootstrap")
                    )
                  )
                )
              )
            ),
            hr(),
            tyrian.Tag(
              "css-separator",
              List(Attribute("renderer", rendererLabel)),
              List(renderer.renderTable(tableState).map(DatatableMsg.TableMsg.apply))
            )
          )
        )
      )
    )
  }
}

object DatatablePlayground {

  // Sample employee data
  val employees: Vector[Employee] = Vector(
    Employee(1, "Alice Smith", "alice@company.com", "Engineering", 95000, LocalDate.of(2020, 3, 15), true),
    Employee(2, "Bob Johnson", "bob@company.com", "Marketing", 75000, LocalDate.of(2019, 7, 22), true),
    Employee(3, "Carol Williams", "carol@company.com", "Engineering", 105000, LocalDate.of(2018, 1, 10), true),
    Employee(4, "David Brown", "david@company.com", "Sales", 65000, LocalDate.of(2021, 11, 5), false),
    Employee(5, "Eva Martinez", "eva@company.com", "Engineering", 98000, LocalDate.of(2020, 6, 1), true),
    Employee(6, "Frank Garcia", "frank@company.com", "HR", 70000, LocalDate.of(2019, 2, 14), true),
    Employee(7, "Grace Lee", "grace@company.com", "Marketing", 82000, LocalDate.of(2017, 9, 30), true),
    Employee(8, "Henry Wilson", "henry@company.com", "Sales", 72000, LocalDate.of(2021, 4, 12), true),
    Employee(9, "Ivy Chen", "ivy@company.com", "Engineering", 110000, LocalDate.of(2016, 8, 20), true),
    Employee(10, "Jack Taylor", "jack@company.com", "HR", 68000, LocalDate.of(2020, 12, 1), false),
    Employee(11, "Karen White", "karen@company.com", "Marketing", 78000, LocalDate.of(2019, 5, 18), true),
    Employee(12, "Leo Anderson", "leo@company.com", "Engineering", 102000, LocalDate.of(2018, 3, 25), true),
    Employee(13, "Maya Thomas", "maya@company.com", "Sales", 69000, LocalDate.of(2021, 7, 8), true),
    Employee(14, "Noah Jackson", "noah@company.com", "Engineering", 97000, LocalDate.of(2020, 1, 30), true),
    Employee(15, "Olivia Harris", "olivia@company.com", "HR", 72000, LocalDate.of(2019, 10, 5), true),
    Employee(16, "Paul Martin", "paul@company.com", "Marketing", 80000, LocalDate.of(2018, 6, 15), false),
    Employee(17, "Quinn Robinson", "quinn@company.com", "Sales", 67000, LocalDate.of(2021, 2, 28), true),
    Employee(18, "Rachel Clark", "rachel@company.com", "Engineering", 108000, LocalDate.of(2017, 4, 10), true),
    Employee(19, "Sam Lewis", "sam@company.com", "HR", 71000, LocalDate.of(2020, 8, 22), true),
    Employee(20, "Tina Walker", "tina@company.com", "Marketing", 84000, LocalDate.of(2019, 11, 12), true),
    Employee(21, "Uma Hall", "uma@company.com", "Engineering", 100000, LocalDate.of(2018, 9, 5), true),
    Employee(22, "Victor Young", "victor@company.com", "Sales", 71000, LocalDate.of(2021, 6, 18), true),
    Employee(23, "Wendy King", "wendy@company.com", "HR", 69000, LocalDate.of(2020, 4, 2), false),
    Employee(24, "Xavier Scott", "xavier@company.com", "Engineering", 99000, LocalDate.of(2019, 1, 15), true),
    Employee(25, "Yara Green", "yara@company.com", "Marketing", 76000, LocalDate.of(2018, 12, 8), true)
  )

  // Table definition using derivation
  val tableDef: TableDef[Employee] = TableDefBuilder[Employee]
    .exclude(_.id)
    .modify(_.name)(_.withFilter(ColumnFilter.text))
    .modify(_.email)(_.withFilter(ColumnFilter.text).withRender(e => e.split("@").head + "@..."))
    .modify(_.department)(_.withFilter(ColumnFilter.select))
    .modify(_.salary)(
      _.withFilter(ColumnFilter.numberRange(s => Some(s)))
        .withRender(s => f"$$$s%,.0f")
        .withSort(Ordering.Double.TotalOrdering)
    )
    .modify(_.hireDate)(
      _.withFilter(ColumnFilter.dateRange(d => Some(d)))
        .withRender(_.toString)
    )
    .modify(_.active)(_.withFilter(ColumnFilter.boolean(identity)).withRender(b => if (b) "Yes" else "No"))
    .rename(_.hireDate, "Hire Date")
    .build("employees")
    .withPageSize(10)
    .withSelection(multi = true)

  def empty(): DatatablePlayground = {
    DatatablePlayground(
      tableState = TableState(tableDef, employees),
      framework = CssFramework.Bulma
    )
  }
}
