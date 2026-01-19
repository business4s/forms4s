package forms4s.datatable

enum SortDirection {
  case Asc, Desc

  def toggle: SortDirection = this match {
    case Asc  => Desc
    case Desc => Asc
  }

  def symbol: String = this match {
    case Asc  => "▲"
    case Desc => "▼"
  }
}
