import play.sbt.Play.autoImport._
import play.sbt.PlayScala
import Dependencies._
import sbt.Keys._
import sbt._


object AtomiumBuild extends Build with BuildSettings {

  import Dependencies._


  javacOptions in Global ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint", "-Xdoclint:none")


  //----------------------------------------------------------------
  lazy val coreModule = {

    val mainDeps = Seq(jacksonDatabind)
    val testDeps = Seq(junit, junitInterface, postgresdriver)

    project("core")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossPaths := false)
      .settings(fork := true) //need to fork because of problem with registering JDBC Driver on repeated test invocation.
      .settings( autoScalaLibrary := false )

  }

  //----------------------------------------------------------------
  lazy val clientScalaModule = {

    val mainDeps = Seq(rxscala)
    val testDeps = mainScalaTestDependencies ++ Seq(wiremock)

    project("client-scala")
      .settings(publishArtifact in Test := true)
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .dependsOn(coreModule, clientJavaModule % "test->test;compile->compile")
      .aggregate(coreModule)
  }

  //----------------------------------------------------------------
  lazy val clientJavaModule = {

    val mainDeps = Seq(slf4j, commonsIo, rxhttpclient)
    val testDeps = Seq(junit, wiremock, mockitoCore, assertJ, junitInterface)

    project("client-java")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( autoScalaLibrary := false )
      .settings(crossPaths := false)
      .dependsOn(coreModule)

  }


  //----------------------------------------------------------------
  lazy val commonPlayModule = {

    val mainDeps = Seq(play, playJson)

    project("common-play")
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(coreModule)
  }

  //----------------------------------------------------------------
  lazy val commonPlay25Module = {

    val mainDeps = Seq(play25, play25Json)

    //set source dir to source dir in commonPlayModule
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/common-play/src/main/scala"))

    project("common-play25")
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .settings( unmanagedSourceDirectories in Compile := sourceDir.value )
      .settings(crossScalaVersions := Seq("2.11.8"))
      .dependsOn(coreModule)
  }

  //----------------------------------------------------------------
  lazy val commonPlay26Module = {

    val mainDeps = Seq(play26, play26Json)

    //set source dir to source dir in commonPlayModule
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/common-play/src/main/scala"))

    project("common-play26")
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .settings( unmanagedSourceDirectories in Compile := sourceDir.value )
      .settings(crossScalaVersions := Seq("2.11.8", "2.12.3"))
      .dependsOn(coreModule)
  }


  //----------------------------------------------------------------
  lazy val serverSpringModule = {

    val mainDeps = Seq(slf4j, lombok, springContext, springTx, jaxRsApi, restEasy)
    val testDeps = Seq(junit, wiremock, mockitoCore, assertJ, junitInterface)

    project("server-spring")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( autoScalaLibrary := false )
      .settings(crossPaths := false)
      .dependsOn(coreModule)
  }



  //----------------------------------------------------------------
  lazy val serverPlayModule = {

    val mainDeps = Seq(filters)
    val testDeps = Seq(playMockWs, playTest, scalaTestPlay) ++ mainScalaTestDependencies

    project("server-play")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.10.4", "2.11.8"))
      .dependsOn(commonPlayModule)
  }

  //----------------------------------------------------------------
  lazy val serverPlay25Module = {

    val mainDeps = Seq()
    val testDeps = Seq(playMockWs, play25Test, scalaTestPlay) ++ mainScalaTestDependencies

    //set source dir to source dir in serverPlaySampleModule
    val sourceDir = (baseDirectory in ThisBuild)( b => Seq( b / "modules/server-play/src/main/scala"))

    project("server-play25")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings( unmanagedSourceDirectories in Compile := sourceDir.value )
      .settings(crossScalaVersions := Seq("2.11.8"))
      .dependsOn(commonPlay25Module)
  }

  //----------------------------------------------------------------
  lazy val serverPlay26Module = {

    val mainDeps = Seq()
    val testDeps = Seq(play26Test, scalaTestPlay26) ++ mainScalaTestDependencies

    project("server-play26")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossScalaVersions := Seq("2.12.3", "2.11.8"))
      .dependsOn(commonPlay26Module)
  }


  //----------------------------------------------------------------
  lazy val main = mainProject(
    coreModule,
    commonPlayModule,
    commonPlay25Module,
    commonPlay26Module,
    clientScalaModule,
    clientJavaModule,
    serverSpringModule,
    serverPlayModule,
    serverPlay25Module,
    serverPlay26Module
  )
}
