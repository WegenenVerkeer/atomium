import Dependencies._

val projectName = "atomium"

organization in ThisBuild := "be.wegenenverkeer"

javacOptions ++= Seq("-source", "11", "-target", "11", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "11")
    sys.error("Java 11 is required for this project.")
}

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.12.8", "2.13.2")
parallelExecution := false

libraryDependencies ++= mainDependencies
scalacOptions in ThisBuild := Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-language:postfixOps"
)

lazy val coreModule = {
  val coreDeps = mainDependencies ++ Seq(jacksonDatabind, junit, junitInterface, postgresdriver)

  Project(
    id   = "atomium-core",
    base = file("modules/core")
  ).settings(libraryDependencies ++= coreDeps)
    .settings(crossPaths := false)
    .settings(fork := true) //need to fork because of problem with registering JDBC Driver on repeated test invocation.
    .settings(sources in (Compile, doc) := Seq()) // workaround: skip javadoc, sbt can't build them
    .settings(autoScalaLibrary := false)
    .settings(PublishingSettings.publishingSettings)
}

lazy val clientJavaModule = Project(
  id   = "atomium-client-v2",
  base = file("modules/client-java")
).settings(
    libraryDependencies ++= Seq(slf4j, rxhttpclient) ++ Seq(junit, wiremock, junitInterface, reactor, reactorTest, reactorAdapter),
    autoScalaLibrary := false,
    crossPaths := false,
    fork := true,
    sources in (Compile, doc) := Seq() // workaround: skip javadoc, sbt can't build them
  )
  .settings(PublishingSettings.publishingSettings)
  .dependsOn(coreModule)

lazy val play26Module = Project(
  id   = "atomium-play26",
  base = file("modules/play26")
).settings(libraryDependencies ++= Seq(play26, play26Json))
  .settings(PublishingSettings.publishingSettings)
  .settings(crossScalaVersions := Seq("2.12.8")) //no scala 2.13 for Play 2.6
  .dependsOn(coreModule)


lazy val main =
  Project(
    id   = projectName,
    base = file(".")
  ).settings(publishArtifact := false)
    .settings(PublishingSettings.publishingSettings)
    .settings(libraryDependencies ++= Seq(junit, junitInterface))
    .aggregate(
      coreModule,
      play26Module,
      clientJavaModule
    )

