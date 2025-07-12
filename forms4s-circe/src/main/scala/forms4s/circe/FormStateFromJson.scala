package forms4s.circe

import forms4s.{FormElementState, FormElementUpdate}
import io.circe.Json

object FormStateFromJson {

  def hydrate(state: FormElementState, json: Json): FormElementState =
    generateUpdates(state, json).foldLeft(state)((s, up) => s.update(up))

  def generateUpdates(state: FormElementState, json: Json): List[FormElementUpdate] = hydrateElement(json, state)

  private def hydrateElement(json: Json, state: FormElementState): List[FormElementUpdate] = {
    state match {
      case _: FormElementState.Text        => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Number      => json.asNumber.map(_.toDouble).toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Checkbox    => json.asBoolean.toList.map(FormElementUpdate.ValueUpdate(_))
      case _: FormElementState.Select      => json.asString.toList.map(FormElementUpdate.ValueUpdate(_))
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
          generateUpdates(x.value.states(toBeUpdated), json).map(update => FormElementUpdate.Nested(toBeUpdated, update))
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
