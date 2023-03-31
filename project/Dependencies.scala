import sbt._
import Keys._

object Dependencies {
  val play26Version = "2.6.2"

  // main deps
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val rxscala = "io.reactivex"   %% "rxscala"        % "0.26.5"
  val jaxwsRt = "com.sun.xml.ws" % "jaxws-rt"        % "2.3.6" pomOnly ()

  //play26 deps
  val play26Json = "com.typesafe.play" %% "play-json" % play26Version
  val play26     = "com.typesafe.play" %% "play"      % play26Version

  // test deps
  val scalaTest             = "org.scalatest" %% "scalatest" % "3.1.2" % "test"
  val scalaTestPlay26       = "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.1" % "test"
  val scalaCheck            = "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"
  val wiremock              = "com.github.tomakehurst" % "wiremock" % "2.26.3" % "test"
  val postgresdriver        = "org.postgresql" % "postgresql" % "42.2.13" % "test"
  val play26Test            = "com.typesafe.play" %% "play-test" % play26Version % "test"
  val testcontainersVersion = "1.15.1"
  val testcontainers        = "org.testcontainers" % "testcontainers" % testcontainersVersion % "test"
  val testcontainersJunit   = "org.testcontainers" % "junit-jupiter" % testcontainersVersion % "test"
  val testcontainersPsql    = "org.testcontainers" % "postgresql" % testcontainersVersion % "test"

  // java deps
  val junit           = "junit"                      % "junit"            % "4.11" % "test"
  val junitInterface  = "com.novocode"               % "junit-interface"  % "0.11" % "test->default"
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.3"
  val rxhttpclient    = "be.wegenenverkeer"          % "rxhttpclient"     % "2.0-RC2"
  val reactor         = "io.projectreactor"          % "reactor-core"     % "3.3.5.RELEASE" % "test"
  val reactorTest     = "io.projectreactor"          % "reactor-test"     % "3.3.3.RELEASE" % "test"
  val reactorAdapter  = "io.projectreactor.addons"   % "reactor-adapter"  % "3.3.3.RELEASE" % "test"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.30"

  val mainDependencies = Seq(
    logback,
    jaxwsRt,
    testcontainers,
    testcontainersJunit,
    testcontainersPsql
  )

  val mainScalaTestDependencies = Seq(
    scalaTest,
    scalaCheck,
    junitInterface
  )
}
