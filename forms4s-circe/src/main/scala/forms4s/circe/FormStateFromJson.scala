package forms4s.circe

import forms4s.{FormElementState, FormElementUpdate}
import io.circe.Json

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

object FormStateFromJson {

  def hydrate(state: FormElementState, json: Json): List[FormElementUpdate] = hydrateElement(json, state)

  private def hydrateElement(json: Json, state: FormElementState): List[FormElementUpdate] = {
    state match {
      case FormElementState.Text(_, _, _)           => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Number(_, _, _)         => json.asNumber.map(_.toDouble).toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Checkbox(_, _, _)       => json.asBoolean.toList.map(FormElementUpdate.ValueUpdate(_))
      case FormElementState.Select(_, _, _)         => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      // TODO this is not correct, need to parse
      case FormElementState.Time(_, _, _)           => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(OffsetTime.parse(x)))
      case FormElementState.Date(_, _, _)           => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(LocalDate.parse(x)))
      case FormElementState.DateTime(_, _, _)       => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(OffsetDateTime.parse(x)))
      case FormElementState.Alternative(elem, s, _) =>
        val selection = for {
          disc        <- elem.discriminator
          obj         <- json.asObject
          field       <- obj(disc)
          fieldStr    <- field.asString
          selectedIdx <- Option(elem.variants.indexWhere(_.core.id == fieldStr)).filter(_ >= 0)
        } yield FormElementUpdate.AlternativeSelected(selectedIdx)
        selection.toList ++ {
          val toBeUpdated = selection.map(_.index).getOrElse(s.selected)
          hydrate(s.states(toBeUpdated), json).map(update => FormElementUpdate.Nested(toBeUpdated, update))
        }

      case FormElementState.Group(_, fields, _) =>
        json.asObject.toList.flatMap { obj =>
          fields.zipWithIndex.flatMap { (sub, idx) =>
            val fieldJson = obj(sub.element.core.id)
            hydrateElement(fieldJson.getOrElse(Json.Null), sub).map { upd =>
              FormElementUpdate.Nested(idx, upd)
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
