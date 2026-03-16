package forms4s

import org.scalatest.freespec.AnyFreeSpec

class ToFormElemTest extends AnyFreeSpec {

  case class Person(name: String, age: Int, isMember: Boolean) derives ToFormElem
  sealed trait Payment derives ToFormElem
  object Payment {
    case class Cash(amount: Double) extends Payment derives ToFormElem

    case class Card(number: String) extends Payment derives ToFormElem
  }

  case class PersonWithDefaults(name: String = "John Doe", age: Int = 30, isMember: Boolean = true) derives ToFormElem

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
          FormElement.Group(
            FormElement.Core("Cash", "Cash", None, List()),
            List(FormElement.Number(FormElement.Core("amount", "Amount", None, List()), false)),
          ),
          FormElement.Group(
            FormElement.Core("Card", "Card", None, List()),
            List(FormElement.Text(FormElement.Core("number", "Number", None, List()), FormElement.Text.Format.Raw)),
          ),
        ),
        None,
      ),
    )
  }

  "default values in form elements" in {
    val formElem = summon[ToFormElem[PersonWithDefaults]].get
    formElem match {
      case FormElement.Group(_, elements) =>
        assert(elements(0).core.defaultValue.contains("John Doe"))
        assert(elements(1).core.defaultValue.contains(30))
        assert(elements(2).core.defaultValue.contains(true))
      case _ => fail("Expected Group")
    }
  }

  "default values in form state" in {
    val formElem = summon[ToFormElem[PersonWithDefaults]].get
    val state = FormElementState.empty(formElem)
    state match {
      case FormElementState.Group(_, values, _, _) =>
        assert(values(0).asInstanceOf[FormElementState.Text].value == "John Doe")
        assert(values(1).asInstanceOf[FormElementState.Number].value.contains(30.0))
        assert(values(2).asInstanceOf[FormElementState.Checkbox].value == true)
      case _ => fail("Expected Group")
    }
  }

}
