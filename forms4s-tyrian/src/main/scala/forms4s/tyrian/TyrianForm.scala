package forms4s.tyrian

import forms4s.{Form, FormElement, FormStylesheet}
import tyrian.Html
import tyrian.Html.*

import scala.collection.mutable

object TyrianForm {

  val defaultStylesheet: FormStylesheet = FormStylesheet()

  sealed trait FormValue

  object FormValue {
    case class Text(value: String) extends FormValue
    case class Checkbox(checked: Boolean) extends FormValue
    case class Select(value: String) extends FormValue
    case class Group(fields: Map[String, FormValue]) extends FormValue
  }

  case class FormState(values: Map[String, FormValue] = Map.empty) {

    def update(name: String, value: FormValue): FormState = {
      val parts = name.split('.')
      if (parts.length == 1) {
        // Simple field
        FormState(values + (name -> value))
      } else {
        // Nested field
        val groupName = parts.head
        val restPath = parts.tail.mkString(".")
        val group = values.getOrElse(groupName, FormValue.Group(Map.empty)) match {
          case g: FormValue.Group => g
          case _ => FormValue.Group(Map.empty)
        }

        val updatedGroup = group.fields.get(restPath.split('.').head) match {
          case Some(nestedGroup: FormValue.Group) =>
            // If the next level is already a group, recursively update it
            val nestedState = FormState(nestedGroup.fields)
            val updatedNestedState = nestedState.update(restPath.split('.').tail.mkString("."), value)
            FormValue.Group(group.fields + (restPath.split('.').head -> FormValue.Group(updatedNestedState.values)))
          case _ =>
            // Otherwise create a new value
            val nestedValue = if (restPath.contains('.')) {
              // If there are more levels, create a nested group
              val nestedPath = restPath.split('.').tail.mkString(".")
              val nestedState = FormState()
              val updatedNestedState = nestedState.update(nestedPath, value)
              FormValue.Group(updatedNestedState.values)
            } else {
              // Simple value at the end of the path
              value
            }
            FormValue.Group(group.fields + (restPath.split('.').head -> nestedValue))
        }

        FormState(values + (groupName -> updatedGroup))
      }
    }

    def getValue(name: String): String = {
      val parts = name.split('.')
      if (parts.length == 1) {
        // Simple field
        values.get(name) match {
          case Some(FormValue.Text(value)) => value
          case Some(FormValue.Checkbox(checked)) => checked.toString
          case Some(FormValue.Select(value)) => value
          case _ => ""
        }
      } else {
        // Nested field
        val groupName = parts.head
        val restPath = parts.tail.mkString(".")
        values.get(groupName) match {
          case Some(FormValue.Group(fields)) =>
            FormState(fields).getValue(restPath)
          case _ => ""
        }
      }
    }
  }

  def render[Msg](
      form: Form,
      state: FormState,
      onUpdate: (String, FormValue) => Msg,
      stylesheet: FormStylesheet = defaultStylesheet,
      renderer: FormRenderer = DefaultFormRenderer,
  ): Html[Msg] = {
    renderer.renderForm(form, state, onUpdate, stylesheet)
  }

}
