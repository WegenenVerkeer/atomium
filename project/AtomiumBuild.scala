import sbt.Keys._
import sbt._

object AtomiumBuild extends Build with BuildSettings {

  import Dependencies._

  javacOptions in Global ++= Seq("-source", "11", "-target", "11")


  //----------------------------------------------------------------
  lazy val coreModule = {
    val mainDeps = Seq(jacksonDatabind)
    val testDeps = Seq(junit, junitInterface, postgresdriver)

    project("core")
      .settings(libraryDependencies ++= mainDeps ++ testDeps)
      .settings(crossPaths := false)
      .settings(fork := true) //need to fork because of problem with registering JDBC Driver on repeated test invocation.
      .settings(sources in (Compile, doc) := Seq()) // workaround: skip javadoc, sbt can't build them
      .settings(autoScalaLibrary := false)
  }

  //----------------------------------------------------------------
  //  lazy val clientScalaModule = {
  //
  //    val mainDeps = Seq(rxscala)
  //    val testDeps = mainScalaTestDependencies ++ Seq(wiremock)
  //
  //    project("client-scala")
  //      .settings(publishArtifact in Test := true)
  //      .settings(libraryDependencies ++= mainDeps ++ testDeps)
  //      .dependsOn(coreModule, clientJavaModule % "test->test;compile->compile")
  //      .aggregate(coreModule)
  //  }

  //----------------------------------------------------------------
  lazy val clientJavaModule = {
    val mainDeps = Seq(slf4j, rxhttpclient)
    val testDeps = Seq(junit, wiremock, junitInterface, reactor, reactorTest, reactorAdapter)

    projectV2("client-java")
      .settings(
        libraryDependencies ++= mainDeps ++ testDeps,
        autoScalaLibrary := false,
        crossPaths := false,
        sources in (Compile, doc) := Seq() // workaround: skip javadoc, sbt can't build them
      )
      .dependsOn(coreModule)
  }


  //  //----------------------------------------------------------------
  //  lazy val play25Module = {
  //
  //    val mainDeps = Seq(play25, play25Json)
  //    val testDeps = Seq(play25Test) ++ mainScalaTestDependencies
  //
  //    project("play25")
  //      .settings(libraryDependencies ++= mainDeps ++ testDeps)
  //      .settings(crossScalaVersions := Seq("2.11.8"))
  //      .dependsOn(coreModule)
  //  }

  //----------------------------------------------------------------
  lazy val play26Module = {

    val mainDeps = Seq(play26, play26Json)

    //set source dir to source dir in commonPlayModule
    val sourceDir = (baseDirectory in ThisBuild) (b => Seq(b / "modules/play25/src/main/scala", b / "modules/play26/src/main/scala"))

    project("play26")
      .settings(libraryDependencies ++= mainDeps ++ mainScalaTestDependencies)
      .settings(unmanagedSourceDirectories in Compile := sourceDir.value)
      .dependsOn(coreModule)
  }


  //----------------------------------------------------------------
  lazy val main = mainProject(
    coreModule,
    play26Module,
    //    clientScalaModule,
    clientJavaModule
  )
}
