import play.Play.autoImport._
import play.PlayScala
import sbt.Keys._
import sbt._


object AtomiumBuild extends Build with BuildSettings {

  import Dependencies._


  javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint")


  //----------------------------------------------------------------
  lazy val javaFormatModule = {

    val mainDeps = Seq(lombok, jacksonDatabind, jacksonJoda)
    val testDeps = Seq(junit)

    project("format-java")
    .settings(libraryDependencies ++= mainDeps ++ testDeps)

  }


  //----------------------------------------------------------------
  lazy val formatModule =
    project("format")
    .dependsOn(javaFormatModule)


  //----------------------------------------------------------------
  lazy val clientScalaModule =
    project("client-scala")
    .settings(publishArtifact in Test := true)
    .dependsOn(formatModule, serverModule)
    .aggregate(formatModule, serverModule)


  //----------------------------------------------------------------
  lazy val commonPlayModule = {

    val mainDeps = Seq(play, playJson)

    project("common-play")
    .settings(libraryDependencies ++= mainDeps)
    .dependsOn(formatModule)
    .aggregate(formatModule)
  }


  //----------------------------------------------------------------
  lazy val serverModule =
    project("server").dependsOn(formatModule)


  //----------------------------------------------------------------
  lazy val serverMongoModule = {

    val mainDeps = Seq(mongoJavaDriver, casbah)
    val testDeps = Seq(embededMongo)

    project("server-mongo")
    .settings(libraryDependencies ++= mainDeps ++ testDeps)
    .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverSlickModule = {

    val mainDeps = Seq(slick, slickPostgres)
    val testDeps = Seq(h2database)

    project("server-slick")
    .settings(libraryDependencies ++= mainDeps ++ testDeps)
    .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverJdbcModule = {

    val testDeps = Seq(h2database)

    project("server-jdbc")
    .settings(libraryDependencies ++= testDeps)
    .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverPlayModule = {

    val mainDeps = Seq(filters)
    val testDeps = Seq(playMockWs, playTest, scalaTestPlay)

    project("server-play")
    .settings(libraryDependencies ++= mainDeps ++ testDeps)
    .enablePlugins(PlayScala)
    .dependsOn(clientScalaModule, serverModule % "test->test;compile->compile", commonPlayModule)
  }


  //----------------------------------------------------------------
  lazy val main = mainProject(
    javaFormatModule,
    formatModule,
    commonPlayModule,
    clientScalaModule,
    serverModule,
    serverMongoModule,
    serverSlickModule,
    serverJdbcModule,
    serverPlayModule
  )
}
