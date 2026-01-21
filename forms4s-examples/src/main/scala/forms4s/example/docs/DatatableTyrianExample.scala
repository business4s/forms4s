package forms4s.example.docs

import cats.effect.IO
import forms4s.datatable.*
import forms4s.tyrian.datatable.*
import tyrian.*

// start_tyrian
case class TableModel(tableState: TableState[Employee])

enum TableMsg {
  case Update(msg: TableUpdate)
}

object DatatableTyrianExample extends TyrianIOApp[TableMsg, TableModel] {

  val tableDef: TableDef[Employee] = ???
  val data: Vector[Employee]       = ???

  def init(flags: Map[String, String]): (TableModel, Cmd[IO, TableMsg]) =
    (TableModel(TableState(tableDef, data)), Cmd.None)

  def update(model: TableModel): TableMsg => (TableModel, Cmd[IO, TableMsg]) = { case TableMsg.Update(msg) =>
    (model.copy(tableState = model.tableState.update(msg)), Cmd.None)
  }

  val renderer: TableRenderer = BulmaTableRenderer

  def view(model: TableModel): Html[TableMsg] =
    renderer.renderTable(model.tableState).map(TableMsg.Update.apply)

  def subscriptions(model: TableModel): Sub[IO, TableMsg] = Sub.None
  def router: Location => TableMsg                        = _ => TableMsg.Update(TableUpdate.ClearAllFilters)
}
// end_tyrian

// start_server_tyrian
object ServerModeExample {
  case class Model(tableState: TableState[Employee])

  enum Msg {
    case TableMsg(msg: TableUpdate)
    case DataLoaded(data: Vector[Employee], totalCount: Int)
    case DataFailed(error: String)
  }

  def fetchFromServer(state: TableState[Employee]): Cmd[IO, Msg] = ???

  def update(model: Model, msg: Msg): (Model, Cmd[IO, Msg]) = msg match {
    case Msg.TableMsg(tableMsg) =>
      val newState = model.tableState.update(tableMsg)
      // Check if this message requires server fetch
      if (tableMsg.needsServerFetch) {
        val loadingState = newState.setLoading
        (model.copy(tableState = loadingState), fetchFromServer(loadingState))
      } else {
        (model.copy(tableState = newState), Cmd.None)
      }

    case Msg.DataLoaded(data, totalCount) =>
      (model.copy(tableState = model.tableState.setServerData(data, totalCount)), Cmd.None)

    case Msg.DataFailed(error) =>
      (model.copy(tableState = model.tableState.setError(error)), Cmd.None)
  }
}
// end_server_tyrian
