lazy val V = _root_.scalafix.sbt.BuildInfo

inThisBuild(
  List(
    organization := "com.nequissimus",
    homepage := Some(url("https://github.com/com/example")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "NeQuissimus",
        "Tim Steinbach",
        "tim@nequissimus.com",
        url("https://nequissimus.com")
      )
    ),
    scalaVersion := V.scala212,
    crossScalaVersions := List(V.scala211, V.scala212, V.scala213),
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List(
      "-Yrangepos",
      "-P:semanticdb:synthetics:on",
      "-deprecation"
    )
  )
)

publish / skip := true

lazy val rules = project.settings(
  moduleName := "sort-imports",
  libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
  pgpPublicRing := file("/tmp/public.asc"),
  pgpSecretRing := file("/tmp/secret.asc"),
  releaseEarlyWith := SonatypePublisher,
  scmInfo := Some(
    ScmInfo(url("https://github.com/NeQuissimus/sort-imports/"), "scm:git:git@github.com:NeQuissimus/sort-imports.git")
  )
)

lazy val input = project.settings(
  publish / skip := true
)

lazy val output = project.settings(
  publish / skip := true
)

lazy val tests = project
  .settings(
    publish / skip := true,
    libraryDependencies += "ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test cross CrossVersion.full,
    Compile / compile :=
      (Compile / compile).dependsOn(input / Compile / compile).value,
    scalafixTestkitOutputSourceDirectories :=
      (output / Compile / sourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (input / Compile / sourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (input / Compile / fullClasspath).value
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
