package forms4s

import org.scalatest.freespec.AnyFreeSpec

class ToFormElemTest extends AnyFreeSpec {

  case class Person(name: String, age: Int, isMember: Boolean) derives ToFormElem
  sealed trait Payment derives ToFormElem
  object Payment {
    case class Cash(amount: Double) extends Payment derives ToFormElem

    case class Card(number: String) extends Payment derives ToFormElem
  }

  "group" in {
    assert(
      summon[ToFormElem[Person]].get == FormElement.Group(
        FormElement.Core("Person", "Person", None, List()),
        List(
          FormElement.Text(FormElement.Core("name", "Name", None, List()), FormElement.Text.Format.Raw),
          FormElement.Number(FormElement.Core("age", "Age", None, List()), true),
          FormElement.Checkbox(FormElement.Core("isMember", "IsMember", None, List())),
        ),
      ),
    )
  }
  "alternative" in {
    assert(
      summon[ToFormElem[Payment]].get == FormElement.Alternative(
        FormElement.Core("", "Payment", None, List()),
        List(
          FormElement.Group(FormElement.Core("Cash", "Cash", None, List()), List(FormElement.Number(FormElement.Core("amount", "Amount", None, List()), false))),
          FormElement.Group(FormElement.Core("Card", "Card", None, List()), List(FormElement.Text(FormElement.Core("number", "Number", None, List()), FormElement.Text.Format.Raw))),
        ),
        None,
      ),
    )
  }

}
