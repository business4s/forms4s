package forms4s.datatable

/** Loading state for server-side data fetching.
  */
enum LoadingState {
  case Idle
  case Loading
  case Failed(message: String)
}
