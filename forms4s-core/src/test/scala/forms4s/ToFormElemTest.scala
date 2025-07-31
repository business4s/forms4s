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
    assert(summon[ToFormElem[Person]].get  == null)
  }
  "alternative" in {
    assert(summon[ToFormElem[Payment]].get  == null)
  }

}
