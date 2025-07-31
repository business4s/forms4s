package forms4s

import scala.compiletime.{constValue, erasedValue, summonInline}
import scala.deriving.Mirror
import forms4s._
import forms4s.FormElement.*

trait ToFormElem[T] {
  def get: FormElement
}

object ToFormElem {

  // ––– Base instances for primitive fields –––

  given ToFormElem[String] with {
    def get: FormElement = Text(emptyCore, format = Text.Format.Raw)
  }

  given ToFormElem[Int] with {
    def get: FormElement = Number(emptyCore, isInteger = true)
  }

  given ToFormElem[Double] with {
    def get: FormElement = Number(emptyCore, isInteger = false)
  }

  given ToFormElem[Boolean] with {
    def get: FormElement = Checkbox(emptyCore)
  }

  private def emptyCore[T]: Core[T] = Core[T]("", "", None, Seq.empty)

  // ––– Derivation for products & sums –––

  inline def derived[T](using m: Mirror.Of[T]): ToFormElem[T] =
    inline m match {
      case p: Mirror.ProductOf[T] => deriveProduct(p)
      case s: Mirror.SumOf[T]     => deriveSum(s)
    }

  class StaticToFormElem[T](elem: FormElement) extends ToFormElem[T] {
    override def get: FormElement = elem
  }

  // –– Derive a case‐class as a Group of its fields –––
  inline def deriveProduct[T](p: Mirror.ProductOf[T]): ToFormElem[T] = {
    // get compile-time labels & ToFormElem instances
    val labels = elemLabels[p.MirroredElemLabels]
    val elems  = summonAll[p.MirroredElemTypes]

    // build a Group
    StaticToFormElem(
      Group(
        core = Core[List[FormElementState]](id = "", label = constValue[p.MirroredLabel], description = None, validators = Seq.empty),
        elements = elems.zip(labels).map { (tf, name) =>
          val fe = tf.get
          // copy into each element its field‐name as id & human label
          updateCore(fe)(id = name, label = name.capitalize)
        },
      ),
    )
  }

  // –– Derive a sealed trait as an Alternative –––
  inline def deriveSum[T](s: Mirror.SumOf[T]): ToFormElem[T] = {
    val variants = summonVariants[s.MirroredElemTypes]
    StaticToFormElem(
      Alternative(
        core = Core[Alternative.State](id = "", label = constValue[s.MirroredLabel], description = None, validators = Seq.empty),
        variants = variants,
        discriminator = None,
      ),
    )
  }

  // –– Helpers: summon all ToFormElem for a Tuple of types –––
  inline def summonAll[T <: Tuple]: List[ToFormElem[?]] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (h *: t)   => summonInline[ToFormElem[h]] :: summonAll[t]
    }

  // –– Helpers: summon all FormElement variants for a sum type –––
  inline def summonVariants[T <: Tuple]: Seq[FormElement] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Seq.empty
      case _: (h *: t)   => summonInline[ToFormElem[h]].get +: summonVariants[t]
    }

  // –– Helpers: get field‐names at compile time –––
  inline def elemLabels[L <: Tuple]: List[String] =
    inline erasedValue[L] match {
      case _: EmptyTuple => Nil
      case _: (h *: t)   => constValue[h].toString :: elemLabels[t]
    }

  // –– Helpers: update the Core.id & Core.label of any FormElement –––
  private def updateCore(fe: FormElement)(id: String, label: String): FormElement = fe match {
    case Text(core, fmt)          => Text(core.copy(id = id, label = label), fmt)
    case Number(core, isInt)      => Number(core.copy(id = id, label = label), isInt)
    case Select(core, opts)       => Select(core.copy(id = id, label = label), opts)
    case Checkbox(core)           => Checkbox(core.copy(id = id, label = label))
    case Group(core, elems)       => Group(core.copy(id = id, label = label), elems)
    case Multivalue(core, item)   => Multivalue(core.copy(id = id, label = label), item)
    case Alternative(core, vs, d) => Alternative(core.copy(id = id, label = label), vs, d)
  }
}
