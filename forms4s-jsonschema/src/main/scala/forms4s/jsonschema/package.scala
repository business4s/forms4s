package forms4s

import sttp.apispec.Schema

package object jsonschema {

  extension (f: FormElement.type ) {

    def fromJsonSchema(schema: Schema) = FormFromJsonSchema.convert(schema)

  }

}
