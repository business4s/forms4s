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
    - [ ] Proper handling of empty and `null` values
    - [ ] Better support for numbers (double vs integer)
    - [ ] Support for time
- Json schema
  - [ ] `oneOf` support for sealed traits
- Validation
  - [x] Infrastructure for field validation
  - [ ] Support for JSON Schema constraints
  - [ ] Integration with `iron` / `neotype`
- Rendering
  - [x] Improved checkbox rendering
- Website and documentation
  - [ ] Basic page
  - [ ] Demo inside docosaurus
  - [ ] Interactive page with scala/jsonschema/form side by side
- [ ] CI/CD
  - [ ] Artifact publishing
  - [ ] scalafix/sbt-tpolecat
- [ ] Derivation from Scala types

---

> Feedback, ideas, or contributions are welcome!
