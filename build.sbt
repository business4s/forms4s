import org.scalajs.linker.interface.ModuleSplitStyle
import org.typelevel.scalacoptions.ScalacOptions

lazy val root = (project in file("."))
  .settings(
    name := "forms4s",
  )
  .aggregate(
    `forms4s-core`.js,
    `forms4s-core`.jvm,
    `forms4s-jsonschema`.js,
    `forms4s-jsonschema`.jvm,
    `forms4s-circe`.js,
    `forms4s-circe`.jvm,
    `forms4s-tyrian`,
    `forms4s-examples`,
  )

lazy val `forms4s-core` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-core"))
  .settings(
    name := "forms4s-core",
    libraryDependencies ++= Seq(
      "io.circe"     %%% "circe-core" % "0.14.14",
      "org.scalatest" %% "scalatest"  % "3.2.19" % "test",
    ),
  )
  .settings(commonSettings)
  .enablePlugins(ScalaJSPlugin)

lazy val `forms4s-jsonschema` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-jsonschema"))
  .settings(
    name := "forms4s-jsonschema",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %%% "apispec-model"      % "0.11.9",
      "com.softwaremill.sttp.tapir"   %%% "tapir-apispec-docs" % "1.11.34" % "test",
    ),
  )
  .settings(commonSettings)
  .dependsOn(`forms4s-core`)

lazy val `forms4s-tyrian` = (project in file("forms4s-tyrian"))
  .settings(
    name := "forms4s-tyrian",
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io" % "0.14.0",
    ),
  )
  .settings(commonSettings)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(`forms4s-core`.js)

lazy val `forms4s-circe` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-circe"))
  .settings(
    name := "forms4s-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.14",
    ),
  )
  .settings(commonSettings)
  .dependsOn(`forms4s-core`)

lazy val `forms4s-examples` =
  (project in file("forms4s-examples"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      name            := "forms4s-examples",
      scalaJSLinkerConfig ~= {
        _.withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(
            ModuleSplitStyle.SmallModulesFor(List("forms4s")),
          )
      },
      autoAPIMappings := true,
      libraryDependencies ++= Seq(
        "com.softwaremill.sttp.tapir"   %%% "tapir-apispec-docs" % "1.11.34",
        "io.circe"                      %%% "circe-parser"       % "0.14.14",
        "com.softwaremill.sttp.apispec" %%% "openapi-circe"      % "0.11.10",
      ),
    )
    .settings(commonSettings)
    .dependsOn(`forms4s-tyrian`, `forms4s-jsonschema`.js, `forms4s-circe`.js)

// tODO check how it got here and if it should be here
Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  organization := "org.business4s",
  scalaVersion := "3.7.1",
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  ),
  // https://users.scala-lang.org/t/scala-js-with-3-7-0-package-scala-contains-object-and-package-with-same-name-caps/10786/5
  dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value
)

lazy val stableVersion = taskKey[String]("stableVersion")
stableVersion := {
  if (isVersionStable.value && !isSnapshot.value) version.value
  else previousStableVersion.value.getOrElse("unreleased")
}

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
