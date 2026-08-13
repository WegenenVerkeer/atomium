import Dependencies._

val projectName = "atomium"

ThisBuild / organization := "be.wegenenverkeer"

javacOptions ++= Seq("-source", "17", "-target", "17", "-Xlint")

scalaVersion := "2.13.16"
parallelExecution := false

libraryDependencies ++= mainDependencies

lazy val coreModule = {
  val coreDeps = mainDependencies ++ Seq(jacksonDatabind, junit, junitInterface, postgresdriver)

  Project(
    id   = "atomium-core",
    base = file("modules/core")
  ).settings(libraryDependencies ++= coreDeps)
    .settings(crossPaths := false)
    .settings(fork := true) //need to fork because of problem with registering JDBC Driver on repeated test invocation.
    .settings(Compile / doc / sources := Seq()) // workaround: skip javadoc, sbt can't build them
    .settings(autoScalaLibrary := false)
    .settings(PublishingSettings.publishingSettings)
}

lazy val clientJavaModule = Project(
  id   = "atomium-client-v2",
  base = file("modules/client-java")
).settings(
    libraryDependencies ++= Seq(slf4j, rxhttpclient) ++ Seq(junit, wiremock, junitInterface, reactor, reactorTest, reactorAdapter, logback),
    autoScalaLibrary := false,
    fork := true,
    Compile / doc / sources := Seq() // workaround: skip javadoc, sbt can't build them
  )
  .settings(crossPaths := false)
  .settings(PublishingSettings.publishingSettings)
  .dependsOn(coreModule)

lazy val main =
  Project(
    id   = projectName,
    base = file(".")
  ).settings(publishArtifact := false)
    .settings(crossPaths := false)
    .settings(PublishingSettings.publishingSettings)
    .settings(libraryDependencies ++= Seq(junit, junitInterface))
    .aggregate(
      coreModule,
      clientJavaModule
    )

