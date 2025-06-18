ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.1"

lazy val root = (project in file("."))
  .settings(
    name := "forms4s",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %% "apispec-model" % "0.11.9",
      "com.softwaremill.sttp.tapir" %% "tapir-apispec-docs" % "1.11.34",
      "org.scalatest" %% "scalatest-freespec" % "3.2.19" % "test",
    )
  )

lazy val `forms4s-tyrian` = (project in file("forms4s-tyrian"))
  .settings(
    name := "forms4s-tyrian",
    libraryDependencies ++= Seq()
  )
  .dependsOn(root)

