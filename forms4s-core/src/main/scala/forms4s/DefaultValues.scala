package forms4s

import scala.quoted.*

object DefaultValues {
  
  inline def extract[T]: Map[String, Any] = ${ extractImpl[T] }

  private def extractImpl[T](using quotes: Quotes, tpe: Type[T]): Expr[Map[String, Any]] = {
    import quotes.reflect.*
    
    val sym = TypeTree.of[T].symbol
    
    // Get the companion module
    val companionSym = sym.companionModule
    if (companionSym == Symbol.noSymbol) {
      return '{ Map.empty[String, Any] }
    }
    
    val comp = sym.companionClass
    val mod = Ref(companionSym)
    
    // Get field names that have defaults
    val names = sym.caseFields.filter(p => p.flags.is(Flags.HasDefault)).map(_.name)
    
    val namesExpr: Expr[List[String]] = Expr.ofList(names.map(Expr(_)))
    
    // Get the companion class body
    val body = comp.tree.asInstanceOf[ClassDef].body
    
    // Find default value methods ($lessinit$greater$default$N)
    val idents: List[Ref] = body.collect {
      case deff @ DefDef(name, _, _, _) if name.startsWith("$lessinit$greater$default") =>
        mod.select(deff.symbol)
    }
    
    val identsExpr: Expr[List[Any]] = Expr.ofList(idents.map(_.asExpr))
    
    '{ $namesExpr.zip($identsExpr).toMap }
  }
}
