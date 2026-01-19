package forms4s.datatable.derivation

import forms4s.datatable.*
import scala.quoted.*

object TableDerivationMacros {

  /**
   * Extract field name from a selector lambda like `_.fieldName`
   */
  def extractFieldNameImpl[T: Type, V: Type](selector: Expr[T => V])(using Quotes): Expr[String] = {
    import quotes.reflect.*

    def extractFromTerm(term: Term): Option[String] = term match {
      case Select(_, fieldName) => Some(fieldName)
      case Inlined(_, _, inner) => extractFromTerm(inner)
      case Block(_, inner)      => extractFromTerm(inner)
      case Lambda(_, body)      => extractFromTerm(body)
      case _                    => None
    }

    def extract(term: Term): String = term match {
      // Pattern: Inlined(_, _, Block(List(DefDef(_, _, _, Some(body))), _))
      case Inlined(_, _, Block(List(DefDef(_, _, _, Some(body))), _)) =>
        extractFromTerm(body).getOrElse(
          report.errorAndAbort(s"Could not extract field name from body: ${body.show}")
        )
      // Pattern: Block(List(DefDef(_, _, _, Some(body))), _)
      case Block(List(DefDef(_, _, _, Some(body))), _) =>
        extractFromTerm(body).getOrElse(
          report.errorAndAbort(s"Could not extract field name from body: ${body.show}")
        )
      // Pattern: Inlined wrapping something else
      case Inlined(_, _, inner) =>
        extract(inner)
      // Direct lambda
      case Lambda(_, body) =>
        extractFromTerm(body).getOrElse(
          report.errorAndAbort(s"Could not extract field name from lambda body: ${body.show}")
        )
      case other =>
        report.errorAndAbort(
          s"Expected a lambda like `_.fieldName`, got: ${other.show}"
        )
    }

    Expr(extract(selector.asTerm))
  }

  /**
   * Derive columns for all fields of a case class.
   */
  def deriveColumnsImpl[T: Type](using Quotes): Expr[List[Column[T, ?]]] = {
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val sym = tpe.typeSymbol

    if (!sym.flags.is(Flags.Case)) {
      report.errorAndAbort(s"TableDefBuilder requires a case class, got ${sym.name}")
    }

    val fields = sym.caseFields

    val columns: List[Expr[Column[T, ?]]] = fields.map { field =>
      val fieldName = field.name
      val fieldLabel = camelToTitle(fieldName)

      field.tree match {
        case ValDef(_, tpt, _) =>
          val fieldType = tpt.tpe
          fieldType.asType match {
            case '[ft] =>
              '{
                Column[T, ft](
                  id = ${ Expr(fieldName) },
                  label = ${ Expr(fieldLabel) },
                  extract = (t: T) => ${ Select.unique('t.asTerm, fieldName).asExprOf[ft] }
                )
              }
          }
      }
    }

    Expr.ofList(columns)
  }

  private def camelToTitle(s: String): String =
    s.replaceAll("([a-z])([A-Z])", "$1 $2").capitalize
}
