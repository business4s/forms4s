package forms4s.circe

import forms4s.{FormElementState, FormElementUpdate}
import io.circe.Json

object FormStateFromJson {

  def hydrate(state: FormElementState, json: Json): List[FormElementUpdate] = hydrateElement(json, state)

  private def hydrateElement(json: Json, state: FormElementState): List[FormElementUpdate] = {
    state match {
      case FormElementState.Text(_, _, _)     => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Number(_, _, _)   => json.asNumber.map(_.toDouble).toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Checkbox(_, _, _) => json.asBoolean.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Select(_, _, _)   => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      // TODO this is not correct, need to parse
      case FormElementState.Time(_, _, _)     => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Date(_, _, _)     => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.DateTime(_, _, _) => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))

      case FormElementState.Group(_, fields, _) =>
        json.asObject.toList.flatMap { obj =>
          fields.flatMap { sub =>
            val fieldJson = obj(sub.element.core.id)
            hydrateElement(fieldJson.getOrElse(Json.Null), sub).map { upd =>
              FormElementUpdate.Nested(sub.element.core.id, upd)
            }
          }
        }

      case FormElementState.Multivalue(_, values, _) =>
        json.asArray.toList.flatMap { arr =>
          arr.zipWithIndex.flatMap { case (itemJson, idx) =>
            values.lift(idx).toList.flatMap { sub =>
              hydrateElement(itemJson, sub).map { upd =>
                FormElementUpdate.MultivalueUpdate(idx, upd)
              }
            }
          }
        }
    }
  }
}
