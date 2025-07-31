package forms4s.example.docs

import forms4s.{FormElement, ToFormElem}

object DerivationExample {

  // start_doc
  case class MyForm(name: String, age: Int) derives ToFormElem

  val formModel: FormElement = summon[ToFormElem[MyForm]].get
  // end_doc

}
