ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / organization := "org.business4s"

ThisBuild / scalaVersion := "3.7.1"

lazy val root = (project in file("."))
  .settings(
    name := "forms4s",
  )
  .aggregate(
    `forms4s-core`,
    `forms4s-jsonschema`,
    `forms4s-tyrian`,
    `forms4s-circe`,
    `forms4s-examples`,
  )

lazy val `forms4s-core` = (project in file("forms4s-core"))
  .settings(
    name := "forms4s-core",
    libraryDependencies ++= Seq(
      "io.circe"     %%% "circe-core"         % "0.14.14",
      "org.scalatest" %% "scalatest-freespec" % "3.2.19" % "test",
    ),
  )
  .enablePlugins(ScalaJSPlugin)

// TODO crossproject
lazy val `forms4s-jsonschema` = (project in file("forms4s-jsonschema"))
  .settings(
    name := "forms4s-jsonschema",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %%% "apispec-model"      % "0.11.9",
      "com.softwaremill.sttp.tapir"    %% "tapir-apispec-docs" % "1.11.34" % "test",
      "org.scalatest"                  %% "scalatest-freespec" % "3.2.19"  % "test",
    ),
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`forms4s-core`)

lazy val `forms4s-tyrian` = (project in file("forms4s-tyrian"))
  .settings(
    name := "forms4s-tyrian",
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io" % "0.14.0",
    ),
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`forms4s-core`)

Global / onChangedBuildSource := ReloadOnSourceChanges

// TODO crossproject
lazy val `forms4s-circe` = (project in file("forms4s-circe"))
  .settings(
    name := "forms4s-circe",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % "0.14.14",
    ),
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`forms4s-core`)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val `forms4s-examples` =
  (project in file("forms4s-examples"))
    .enablePlugins(ScalaJSPlugin)
    .settings( // Normal settings
      name            := "forms4s-examples",
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.CommonJSModule)
      },
      autoAPIMappings := true,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.tapir" %%% "tapir-apispec-docs" % "1.11.34",
      ),
    )
    .dependsOn(`forms4s-tyrian`, `forms4s-jsonschema`, `forms4s-circe`)
