# forms4s

**forms4s** is a work-in-progress Scala library for building user interfaces from data models and JSON Schema.

## Features

- 🔧 Generate UI components (e.g. for Tyrian) from structured schemas
- 🧾 Derive form structure from JSON Schema
- 📤 Extract user input as JSON data

## Roadmap / TODO

- Core
  - [x] Multi-value field support (`List[_]`)
  - [x] Hydrate form with json (e.g. for persistent urls or other cache)
  - [x] Better support for numbers (double vs integer, empty value)
  - [ ] Adjustment of forms
  - [ ] Derivation from Scala types
  - Support for more sophisticated types
    - [x] Time
    - [x] sealed traits (`oneOf` in json schema)
    - [ ] Duration (requires custom input)
    - [ ] Email (no scala/java type)
    - [ ] uuid (no special html input, just validation)
- Rendering
  - [x] Specialize on bulma
  - [x] Bootstrap renderer
  - [x] Raw/Pico renderer
  - [x] Improved checkbox rendering
  - [ ] Rendering help/description
- Validation
  - [x] Infrastructure for field validation
  - [ ] Support for JSON Schema constraints
  - [ ] Integration with `iron` / `neotype`
- Website and documentation
  - [x] Basic page
  - [x] Demo inside docosaurus
    - [ ] Auto build, even on local (yarn task)
  - [ ] Proper docs
    - How to set initial values
  - [x] Interactive page with scala/jsonschema/form side by side
    - [ ] Ability to edit code
- [ ] CI/CD
  - [ ] Artifact publishing
  - [ ] scalafix/sbt-tpolecat

---

> Feedback, ideas, or contributions are welcome!
