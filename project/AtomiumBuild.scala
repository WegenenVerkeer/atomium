import play.Play.autoImport._
import play.PlayScala
import sbt.Keys._
import sbt._


object AtomiumBuild extends Build with BuildSettings {

  import Dependencies._


  javacOptions in Global ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-Xdoclint:none")


  //----------------------------------------------------------------
  lazy val javaFormatModule = {

    val mainDeps = Seq(jacksonDatabind, jacksonJoda)
    val testDeps = Seq(junit)

    project("format-java")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( autoScalaLibrary := false )
      .settings(crossPaths := false)

  }


  //----------------------------------------------------------------
  lazy val formatModule =
    project("format")
      .dependsOn(javaFormatModule)


  //----------------------------------------------------------------
  lazy val clientAkkaModule = {

    val mainDeps = Seq(rxscala, play, playJson, akkaPersistence)
    val testDeps = mainScalaTestDependencies ++ Seq(wiremock)

    project("client-akka")
      .settings(publishArtifact in Test := true)
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(formatModule, serverModule, clientJavaModule % "test->test;compile->compile", clientScalaModule % "test->test;compile->compile")
      .aggregate(formatModule, serverModule)
  }

  //----------------------------------------------------------------
  lazy val clientScalaModule = {

    val mainDeps = Seq(rxscala)
    val testDeps = mainScalaTestDependencies ++ Seq(wiremock)

    project("client-scala")
      .settings(publishArtifact in Test := true)
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(formatModule, serverModule, clientJavaModule % "test->test;compile->compile")
      .aggregate(formatModule, serverModule)
  }

  //----------------------------------------------------------------
  lazy val clientJavaModule = {

    val mainDeps = Seq(slf4j, commonsIo, rxhttpclient)
    val testDeps = Seq(junit, wiremock, mockitoCore, assertJ, jfakerMockito, junitInterface)

    project("client-java")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( autoScalaLibrary := false )
      .settings(crossPaths := false)
      .dependsOn(javaFormatModule)

  }


  //----------------------------------------------------------------
  lazy val commonPlayModule = {

    val mainDeps = Seq(play, playJson)

    project("common-play")
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .dependsOn(formatModule)
      .aggregate(formatModule)
  }


  //----------------------------------------------------------------
  lazy val serverModule =
    project("server")
      .settings(libraryDependencies ++= mainScalaTestDependencies)
      .dependsOn(formatModule)


  //----------------------------------------------------------------
  lazy val serverMongoModule = {

    val mainDeps = Seq(mongoJavaDriver, casbah)
    val testDeps = Seq(embededMongo) ++ mainScalaTestDependencies

    project("server-mongo")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverSlickModule = {

    val mainDeps = Seq(slick, slickPostgres)
    val testDeps = Seq(h2database) ++ mainScalaTestDependencies

    project("server-slick")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverJdbcModule = {

    val testDeps = Seq(h2database) ++ mainScalaTestDependencies

    project("server-jdbc")
      .settings(libraryDependencies ++= testDeps)
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverPlayModule = {

    val mainDeps = Seq(filters)
    val testDeps = Seq(playMockWs, playTest, scalaTestPlay) ++ mainScalaTestDependencies

    project("server-play")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(serverModule % "test->test;compile->compile", commonPlayModule)
  }

 //----------------------------------------------------------------
  lazy val serverPlaySampleModule = {
   
    project("server-play-sample")
      .enablePlugins(PlayScala)
      .dependsOn(serverModule % "test->test;compile->compile", serverPlayModule)
  }


  //----------------------------------------------------------------
  lazy val main = mainProject(
    javaFormatModule,
    formatModule,
    commonPlayModule,
    clientAkkaModule,
    clientScalaModule,
    clientJavaModule,
    serverModule,
    serverMongoModule,
    serverSlickModule,
    serverJdbcModule,
    serverPlayModule,
    serverPlaySampleModule
  )
}
