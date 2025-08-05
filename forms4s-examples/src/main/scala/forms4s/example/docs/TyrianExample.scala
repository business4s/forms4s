package forms4s.example.docs

import cats.effect.IO
import forms4s.tyrian.FormRenderer
import forms4s.{FormElement, FormElementState, FormElementUpdate}
import tyrian.*

// start_doc
case class MyModel(form: FormElementState)

enum MyMsg {
  case FormUpdate(msg: FormElementUpdate)
  case NoOp
}

object TyrianExample extends TyrianIOApp[MyMsg, MyModel] {

  val myForm: FormElement = ???

  def init(flags: Map[String, String]): (MyModel, Cmd[IO, MyMsg]) = {
    (MyModel(FormElementState.empty(myForm)), Cmd.None)
  }

  def update(model: MyModel): MyMsg => (MyModel, Cmd[IO, MyMsg]) = {
    case MyMsg.FormUpdate(msg) => (MyModel(model.form.update(msg)), Cmd.None)
    case MyMsg.NoOp            => (model, Cmd.None)
  }

  val renderer: FormRenderer            = ???
  def view(model: MyModel): Html[MyMsg] =
    renderer.renderForm(model.form).map(update => MyMsg.FormUpdate(update))

  def subscriptions(model: MyModel): Sub[IO, MyMsg] = Sub.None
  def router: Location => MyMsg                     = _ => MyMsg.NoOp

}
// end_doc
