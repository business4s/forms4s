package forms4s

object ExampleModel {
  case class User(name: String, age: Int, address: Address)

  case class Address(street: String, city: String)
}