---
sidebar_position: 2
---

# Getting Started

To use forms4s in your project, add the following dependencies:

```scala
libraryDependencies ++= Seq(
  "io.github.forms4s" %% "forms4s-core"       % "<version>", // Core functionality
  "io.github.forms4s" %% "forms4s-jsonschema" % "<version>", // JSON Schema support
  "io.github.forms4s" %% "forms4s-circe"      % "<version>", // JSON handling
  "io.github.forms4s" %% "forms4s-tyrian"     % "<version>", // UI rendering
)
```

## Basic Usage

Here's a simple example of how to use forms4s:

```scala file=./main/scala/forms4s/example/docs/GettingStarted.scala start=start_doc end=end_doc
```
