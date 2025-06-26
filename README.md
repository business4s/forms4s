# forms4s

**forms4s** is a work-in-progress Scala library for building user interfaces from data models and JSON Schema.

## ✨ Features

- 🔧 Generate UI components (e.g. for Tyrian) from structured schemas
- 🧾 Derive form structure from JSON Schema
- 📤 Extract user input as JSON data

## 🚧 Roadmap / TODO

- [x] Multi-value field support (`List[_]`)
- [ ] Proper handling of empty and `null` values
- [ ] Better support for numbers (double vs integer)
- [ ] Support for time
- [ ] Validation
  - [ ] Infrastructure for field validation
  - [ ] Support for JSON Schema constraints
  - [ ] Integration with `iron` / `neotype`
- [ ] Improved checkbox rendering
- [ ] `oneOf` support for sealed traits
- [ ] Website and documentation
  - [ ] cool interactive page with scala/jsonschema/form side by side
- [ ] CI/CD
  - [ ] Artifact publishing
  - [ ] scalafix/sbt-tpolecat
- [ ] Derivation from Scala types
- [ ] Hydrate form with json (e.g. for persistent urls or other cache)

---

> Feedback, ideas, or contributions are welcome!
