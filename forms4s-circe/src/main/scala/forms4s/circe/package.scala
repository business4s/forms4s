package forms4s

import io.circe.Json

package object circe {

  extension (s: FormElementState) {
    def extractJson: Json = FormStateToJson.extract(s)
  }
  extension (s: FormElementState) {
    def load(json: Json): FormElementState = FormStateFromJson.hydrate(s, json)
  }

}
