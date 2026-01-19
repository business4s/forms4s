package forms4s.datatable.derivation

import forms4s.datatable.*
import scala.deriving.Mirror

/** Builder for customizing derived table definitions.
  *
  * Usage:
  * {{{
  * case class User(id: Int, name: String, email: String, password: String)
  *
  * val tableDef = TableDefBuilder[User]
  *   .exclude(_.password)           // Exclude password column
  *   .modify(_.name)(_.withFilter(ColumnFilter.text))
  *   .modify(_.email)(_.withRender(e => e.take(20) + "..."))
  *   .rename(_.id, "User ID")
  *   .build("users")
  * }}}
  */
case class TableDefBuilder[T](
    private val exclusions: Set[String],
    private val modifications: Map[String, Column[T, ?] => Column[T, ?]],
    private val renames: Map[String, String],
    private val additions: List[Column[T, ?]],
)(using m: Mirror.ProductOf[T]) {

  /** Exclude a field from the table. Uses a lambda to select the field in a type-safe way.
    */
  inline def exclude[V](inline selector: T => V): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(exclusions = exclusions + fieldName)
  }

  /** Modify a column configuration.
    */
  inline def modify[V](inline selector: T => V)(f: Column[T, V] => Column[T, V]): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(modifications = modifications + (fieldName -> f.asInstanceOf[Column[T, ?] => Column[T, ?]]))
  }

  /** Rename a column.
    */
  inline def rename[V](inline selector: T => V, newLabel: String): TableDefBuilder[T] = {
    val fieldName = extractFieldName(selector)
    copy(renames = renames + (fieldName -> newLabel))
  }

  /** Add a computed column.
    */
  def addColumn[V](col: Column[T, V]): TableDefBuilder[T] =
    copy(additions = additions :+ col)

  /** Build the final TableDef.
    */
  inline def build(id: String): TableDef[T] = {
    val baseColumns     = deriveColumnsWithMacro[T]
    val filteredColumns = baseColumns.filterNot(c => exclusions.contains(c.id))
    val modifiedColumns = filteredColumns.map { col =>
      val renamed = renames.get(col.id).fold(col)(newLabel => col.asInstanceOf[Column[T, Any]].copy(label = newLabel).asInstanceOf[Column[T, ?]])
      modifications.get(col.id).fold(renamed)(f => f(renamed))
    }
    TableDef(id, columns = modifiedColumns ++ additions)
  }

  // Macro helper to extract field name from lambda
  private inline def extractFieldName[V](inline selector: T => V): String =
    ${ TableDerivationMacros.extractFieldNameImpl[T, V]('selector) }

  // Macro helper to derive columns
  private inline def deriveColumnsWithMacro[U]: List[Column[U, ?]] =
    ${ TableDerivationMacros.deriveColumnsImpl[U] }
}

object TableDefBuilder {
  inline def apply[T](using m: Mirror.ProductOf[T]): TableDefBuilder[T] =
    new TableDefBuilder[T](
      exclusions = Set.empty,
      modifications = Map.empty,
      renames = Map.empty,
      additions = Nil,
    )
}
