import play.sbt.Play.autoImport._
import play.sbt.PlayScala
import Dependencies._
import sbt.Keys._
import sbt._


object AtomiumBuild extends Build with BuildSettings {

  import Dependencies._


  javacOptions in Global ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-Xdoclint:none")


  //----------------------------------------------------------------
  lazy val javaFormatModule = {

    val mainDeps = Seq(jacksonDatabind, jacksonJavaTime)
    val testDeps = Seq(junit, junitInterface)

    project("format-java")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossPaths := false)
      .settings( autoScalaLibrary := false )

  }

  //----------------------------------------------------------------
  lazy val clientScalaModule = {

    val mainDeps = Seq(rxscala)
    val testDeps = mainScalaTestDependencies ++ Seq(wiremock)

    project("client-scala")
      .settings(publishArtifact in Test := true)
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(javaFormatModule, serverModule, clientJavaModule % "test->test;compile->compile")
      .aggregate(javaFormatModule, serverModule)
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
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(javaFormatModule)
  }

  //----------------------------------------------------------------
  lazy val commonPlay25Module = {

    val mainDeps = Seq(play25, play25Json)

    //set source dir to source dir in commonPlayModule
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/common-play/src/main/scala"))     

    project("common-play25")          
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .settings( unmanagedSourceDirectories in Compile <<= sourceDir )
      .settings(crossScalaVersions := Seq("2.11.8"))    
      .dependsOn(javaFormatModule)
  }

  //----------------------------------------------------------------
  lazy val serverModule =
    project("server")
      .settings(libraryDependencies ++= mainScalaTestDependencies)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(javaFormatModule)


  //----------------------------------------------------------------
  lazy val serverMongoModule = {

    val mainDeps = Seq(mongoJavaDriver, casbah)
    val testDeps = Seq(embededMongo) ++ mainScalaTestDependencies

    project("server-mongo")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverSlickModule = {

    val mainDeps = Seq(slick, slickPostgres)
    val testDeps = Seq(h2database) ++ mainScalaTestDependencies

    project("server-slick")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverJdbcModule = {

    val testDeps = Seq(h2database) ++ mainScalaTestDependencies

    project("server-jdbc")
      .settings(libraryDependencies ++= testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile")
  }


  //----------------------------------------------------------------
  lazy val serverPlayModule = {

    val mainDeps = Seq(filters)
    val testDeps = Seq(playMockWs, playTest, scalaTestPlay) ++ mainScalaTestDependencies

    project("server-play")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile", commonPlayModule)
  }

  //----------------------------------------------------------------
  lazy val serverPlay25Module = {

    val mainDeps = Seq()
    val testDeps = Seq(playMockWs, play25Test, scalaTestPlay) ++ mainScalaTestDependencies

    //set source dir to source dir in serverPlaySampleModule
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/server-play/src/main/scala"))     

    project("server-play25")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( unmanagedSourceDirectories in Compile <<= sourceDir )
      .settings(crossScalaVersions := Seq("2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile", commonPlay25Module)
  }

  //----------------------------------------------------------------
  lazy val serverPlaySampleModule = {

    project("server-play-sample")
      .enablePlugins(PlayScala)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(serverModule % "test->test;compile->compile", serverPlayModule)
  }


  //----------------------------------------------------------------
  lazy val main = mainProject(
    javaFormatModule,
    commonPlayModule,
    commonPlay25Module,
    clientScalaModule,
    clientJavaModule,
    serverModule,
    serverMongoModule,
    serverSlickModule,
    serverJdbcModule,
    serverPlayModule,
    serverPlay25Module,
    serverPlaySampleModule
  )
}
