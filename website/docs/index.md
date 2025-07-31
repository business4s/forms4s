---
sidebar_position: 1
---

# Intro

**forms4s** is a Scala library for building user interfaces from data models and JSON Schema. It provides a type-safe
way to define, render, and manage forms in Scala applications.

## Features

- **Generic form model** - Define form structure through an abstract model
- **Generic derivation** - Derive the model from Scala types
- **JSON Schema integration** - Derive the model from JSON Schema
- **Generate UI components** - Create UI components (e.g., for Tyrian) from structured schemas
- **JSON data extraction** - Extract user input as JSON data
- **JSON data hydration** - Fill the form with JSON data
- **Validation** - Validate form input based on schema constraints or custom logic

```mermaid
graph LR
    A[Scala model] -- tapir --> B[Json Schema]
    B --> C[Form Model]
    A -- derivation --> C
    C --> D[Form State]
    D -- render --> E[Web UI]
    E --> F[State Updates]
    F --> D
    D -- extract --> G[JSON]
    G -- load --> D
```