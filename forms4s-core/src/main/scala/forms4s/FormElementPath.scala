package forms4s

import scala.annotation.targetName

case class FormElementPath(raw: Vector[String]) {

  @targetName("append")
  infix def /(segment: String): FormElementPath = new FormElementPath(raw :+ segment)

  override def toString: String = raw.mkString(".")
  def asHtmlId: String          = raw.mkString("__")
}

object FormElementPath {

  val Root = FormElementPath(Vector.empty)

}
