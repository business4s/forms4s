package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

import scala.collection.mutable

object TyrianForm {

  val defaultStylesheet: FormStylesheet = FormStylesheet()

  case class FormState(values: Map[String, String] = Map.empty) {

    def update(name: String, value: String): FormState =
      FormState(values + (name -> value))

    def getValue(name: String): String =
      values.getOrElse(name, "")
  }

  def render[Msg](
      form: Form,
      state: FormState,
      onUpdate: (String, String) => Msg,
      stylesheet: FormStylesheet = defaultStylesheet,
      renderer: FormRenderer = DefaultFormRenderer,
  ): Html[Msg] = {
    renderer.renderForm(form, state, onUpdate, stylesheet)
  }

  def extractData(form: Form, state: FormState): Map[String, Any] = {
    val result = mutable.Map[String, Any]()

    def processElement(element: FormElement, prefix: String): Unit = {
      element match {
        case subform: FormElement.Subform =>
          val fullName  = if prefix.isEmpty then subform.name else s"$prefix.${subform.name}"
          val subResult = mutable.Map[String, Any]()
          subform.form.elements.foreach(subElement => processSubElement(subElement, fullName, subResult))
          if prefix.isEmpty then {
            result(subform.name) = subResult.toMap
          } else {
            val parts   = prefix.split('.')
            var current = result
            for i <- 0 until parts.length - 1 do {
              if !current.contains(parts(i)) then {
                current(parts(i)) = mutable.Map[String, Any]()
              }
              current = current(parts(i)).asInstanceOf[mutable.Map[String, Any]]
            }
            if !current.contains(parts.last) then {
              current(parts.last) = mutable.Map[String, Any]()
            }
            val lastMap = current(parts.last).asInstanceOf[mutable.Map[String, Any]]
            lastMap(subform.name) = subResult.toMap
          }

        case text: FormElement.Text =>
          val fullName = if prefix.isEmpty then text.name else s"$prefix.${text.name}"
          val value    = state.getValue(fullName)
          if prefix.isEmpty then {
            result(text.name) = value
          } else {
            val parts   = prefix.split('.')
            var current = result
            for i <- 0 until parts.length - 1 do {
              if !current.contains(parts(i)) then {
                current(parts(i)) = mutable.Map[String, Any]()
              }
              current = current(parts(i)).asInstanceOf[mutable.Map[String, Any]]
            }
            if !current.contains(parts.last) then {
              current(parts.last) = mutable.Map[String, Any]()
            }
            val lastMap = current(parts.last).asInstanceOf[mutable.Map[String, Any]]
            lastMap(text.name) = value
          }

        case select: FormElement.Select =>
          val fullName = if prefix.isEmpty then select.name else s"$prefix.${select.name}"
          val value    = state.getValue(fullName)
          if prefix.isEmpty then {
            result(select.name) = value
          } else {
            val parts   = prefix.split('.')
            var current = result
            for i <- 0 until parts.length - 1 do {
              if !current.contains(parts(i)) then {
                current(parts(i)) = mutable.Map[String, Any]()
              }
              current = current(parts(i)).asInstanceOf[mutable.Map[String, Any]]
            }
            if !current.contains(parts.last) then {
              current(parts.last) = mutable.Map[String, Any]()
            }
            val lastMap = current(parts.last).asInstanceOf[mutable.Map[String, Any]]
            lastMap(select.name) = value
          }

        case checkbox: FormElement.Checkbox =>
          val fullName = if prefix.isEmpty then checkbox.name else s"$prefix.${checkbox.name}"
          val value    = state.getValue(fullName)
          if prefix.isEmpty then {
            result(checkbox.name) = value
          } else {
            val parts   = prefix.split('.')
            var current = result
            for i <- 0 until parts.length - 1 do {
              if !current.contains(parts(i)) then {
                current(parts(i)) = mutable.Map[String, Any]()
              }
              current = current(parts(i)).asInstanceOf[mutable.Map[String, Any]]
            }
            if !current.contains(parts.last) then {
              current(parts.last) = mutable.Map[String, Any]()
            }
            val lastMap = current(parts.last).asInstanceOf[mutable.Map[String, Any]]
            lastMap(checkbox.name) = value
          }
      }
    }

    def processSubElement(element: FormElement, prefix: String, result: mutable.Map[String, Any]): Unit = {
      element match {
        case subform: FormElement.Subform =>
          val subResult = mutable.Map[String, Any]()
          subform.form.elements.foreach(subElement => processSubElement(subElement, s"$prefix.${subform.name}", subResult))
          result(subform.name) = subResult.toMap

        case text: FormElement.Text =>
          val fullName = s"$prefix.${text.name}"
          val value    = state.getValue(fullName)
          result(text.name) = value

        case select: FormElement.Select =>
          val fullName = s"$prefix.${select.name}"
          val value    = state.getValue(fullName)
          result(select.name) = value

        case checkbox: FormElement.Checkbox =>
          val fullName = s"$prefix.${checkbox.name}"
          val value    = state.getValue(fullName)
          result(checkbox.name) = value
      }
    }

    form.elements.foreach(element => processElement(element, ""))

    result.toMap
  }
}
