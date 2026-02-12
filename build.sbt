import org.scalajs.linker.interface.ModuleSplitStyle
import org.typelevel.scalacoptions.ScalacOptions

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(name := "forms4s")
  .aggregate(
    `forms4s-core`.js,
    `forms4s-core`.jvm,
    `forms4s-jsonschema`.js,
    `forms4s-jsonschema`.jvm,
    `forms4s-circe`.js,
    `forms4s-circe`.jvm,
    `forms4s-tyrian`.js,
    `forms4s-tyrian`.jvm,
    `forms4s-examples`.js,
    `forms4s-examples`.jvm,
  )

lazy val `forms4s-core` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-core"))
  .settings(
    name := "forms4s-core",
    libraryDependencies ++= Seq(
      "io.circe"      %%% "circe-core" % "0.14.15",
      "org.scalatest" %%% "scalatest"  % "3.2.19" % "test",
    ),
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.6.0",
  )
  .settings(commonSettings)

lazy val `forms4s-jsonschema` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-jsonschema"))
  .settings(
    name := "forms4s-jsonschema",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.apispec" %%% "apispec-model"      % "0.11.10",
      "com.softwaremill.sttp.apispec" %%% "openapi-circe"      % "0.11.10",
      "io.circe"                      %%% "circe-parser"       % "0.14.15",
      "com.softwaremill.sttp.tapir"   %%% "tapir-apispec-docs" % "1.11.50" % "test",
    ),
  )
  .settings(commonSettings)
  .dependsOn(`forms4s-core`)

lazy val `forms4s-tyrian` =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("forms4s-tyrian"))
    .settings(
      name := "forms4s-tyrian",
      libraryDependencies ++= Seq(
        "io.indigoengine" %%% "tyrian-io" % "0.14.0",
      ),
    )
    .settings(commonSettings)
    .enablePlugins(ScalaJSPlugin)
    .dependsOn(`forms4s-core`)

lazy val `forms4s-circe` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-circe"))
  .settings(
    name := "forms4s-circe",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core" % "0.14.15",
    ),
  )
  .settings(commonSettings)
  .dependsOn(`forms4s-core`)

lazy val `forms4s-examples` = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("forms4s-examples"))
  .settings(commonSettings)
  .settings(
    name := "forms4s-examples",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"   %%% "tapir-apispec-docs" % "1.11.50",
      "io.circe"                      %%% "circe-parser"       % "0.14.15",
      "com.softwaremill.sttp.apispec" %%% "openapi-circe"      % "0.11.10",
    ),
  )
  .jsSettings(
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(
          ModuleSplitStyle.SmallModulesFor(List("forms4s")),
        )
    },
  )
  .dependsOn(`forms4s-tyrian`, `forms4s-jsonschema`, `forms4s-circe`)

lazy val commonSettings = Seq(
  organization                            := "org.business4s",
  scalaVersion                            := "3.7.1",
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  ),
  // https://users.scala-lang.org/t/scala-js-with-3-7-0-package-scala-contains-object-and-package-with-same-name-caps/10786/5
  dependencyOverrides += "org.scala-lang" %% "scala3-library" % scalaVersion.value,
  scalacOptions ++= Seq("-no-indent"),
  homepage                                := Some(url("https://business4s.github.io/forms4s/")),
  licenses                                := List(License.MIT),
  developers                              := List(
    Developer(
      "Krever",
      "Voytek Pituła",
      "w.pitula@gmail.com",
      url("https://v.pitula.me"),
    ),
  ),
  versionScheme                           := Some("semver-spec"),
)

lazy val stableVersion = taskKey[String]("stableVersion")
stableVersion := {
  if (isVersionStable.value && !isSnapshot.value) version.value
  else previousStableVersion.value.getOrElse(version.value)
}

ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

Global / onChangedBuildSource := ReloadOnSourceChanges
