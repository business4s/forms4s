package forms4s

enum FormValue {
  case Text(value: String)
  case Number(value: Double)
  case Checkbox(checked: Boolean)
  case Select(value: String)
  case Nested(field: String, value: FormValue)
  case MultivalueUpdate(index: Int, value: FormValue)
  case MultivalueAppend()
  case MultivalueRemove(index: Int)
}
