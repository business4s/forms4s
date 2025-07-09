package forms4s.circe

import forms4s.{FormElementState, FormElementUpdate}
import io.circe.Json

import java.time.{LocalDate, OffsetDateTime, OffsetTime}

object FormStateFromJson {

  def hydrate(state: FormElementState, json: Json): List[FormElementUpdate] = hydrateElement(json, state)

  private def hydrateElement(json: Json, state: FormElementState): List[FormElementUpdate] = {
    state match {
      case _: FormElementState.Text        => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Number      => json.asNumber.map(_.toDouble).toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Checkbox    => json.asBoolean.toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Select      => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      // TODO this is not correct, need to parse
      case _: FormElementState.Time        => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(OffsetTime.parse(x)))
      case _: FormElementState.Date        => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(LocalDate.parse(x)))
      case _: FormElementState.DateTime    => json.asString.toList.map(x => FormElementUpdate.ValueUpdate(OffsetDateTime.parse(x)))
      case x: FormElementState.Alternative =>
        val selection = for {
          disc        <- x.element.discriminator
          obj         <- json.asObject
          field       <- obj(disc)
          fieldStr    <- field.asString
          selectedIdx <- Option(x.element.variants.indexWhere(_.core.id == fieldStr)).filter(_ >= 0)
        } yield FormElementUpdate.AlternativeSelected(selectedIdx)
        selection.toList ++ {
          val toBeUpdated = selection.map(_.index).getOrElse(x.value.selected)
          hydrate(x.value.states(toBeUpdated), json).map(update => FormElementUpdate.Nested(toBeUpdated, update))
        }

      case x: FormElementState.Group =>
        json.asObject.toList.flatMap { obj =>
          x.value.zipWithIndex.flatMap { (sub, idx) =>
            val fieldJson = obj(sub.element.core.id)
            hydrateElement(fieldJson.getOrElse(Json.Null), sub).map { upd =>
              FormElementUpdate.Nested(idx, upd)
            }
          }
        }

      case x: FormElementState.Multivalue =>
        json.asArray.toList.flatMap { arr =>
          arr.zipWithIndex.flatMap { case (itemJson, idx) =>
            x.value.lift(idx).toList.flatMap { sub =>
              hydrateElement(itemJson, sub).map { upd =>
                FormElementUpdate.MultivalueUpdate(idx, upd)
              }
            }
          }
        }
    }
  }
}
