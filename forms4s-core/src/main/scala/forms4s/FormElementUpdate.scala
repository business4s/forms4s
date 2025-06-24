package forms4s

enum FormElementUpdate {
  case Text(value: String)
  case Number(value: Double)
  case Checkbox(checked: Boolean)
  case Select(value: String)
  case Nested(field: String, value: FormElementUpdate)
  case MultivalueUpdate(index: Int, value: FormElementUpdate)
  case MultivalueAppend()
  case MultivalueRemove(index: Int)
}
