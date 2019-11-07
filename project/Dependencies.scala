
import sbt._

object Dependencies {

  val play25Version = "2.5.4"
  val play26Version = "2.6.2"
  val play27Version = "2.7.3"

  // main deps
  val logback           = "ch.qos.logback"              %     "logback-classic"               % "1.1.1"
  val rxscala           = "io.reactivex"                %%    "rxscala"                       % "0.26.5"

  //play26 deps
  val play26Json          = "com.typesafe.play"           %%    "play-json"                     % play26Version
  val play26              = "com.typesafe.play"           %%    "play"                          % play26Version
  val play26Test          = "com.typesafe.play"           %%    "play-test"                     % play26Version    % "test"

  //play27 deps
  val play27Json          = "com.typesafe.play"           %%    "play-json"                     % "2.7.1"
  val play27              = "com.typesafe.play"           %%    "play"                          % play27Version
  val play27Test          = "com.typesafe.play"           %%    "play-test"                     % play27Version    % "test"

  // test deps
  val scalaTest         = "org.scalatest"               %%    "scalatest"               % "3.0.1"        % "test"
  val scalaTestPlay26   = "org.scalatestplus.play"      %%    "scalatestplus-play"      % "3.1.1"        % "test"
  val scalaCheck        = "org.scalacheck"              %%    "scalacheck"              % "1.13.4"       % "test"
  val wiremock          = "com.github.tomakehurst"      %     "wiremock"                % "1.57"         % "test"
  val postgresdriver    = "org.postgresql"              %     "postgresql"              % "9.4-1200-jdbc41" % "test"

  // java deps
  val junit             = "junit"                           %   "junit"                    % "4.11"    % "test"
  val junitInterface    = "com.novocode"                    %   "junit-interface"          % "0.11"    % "test->default"
  val jacksonDatabind   = "com.fasterxml.jackson.core"      %   "jackson-databind"         % "2.4.3"
  val rxhttpclient      = "be.wegenenverkeer"               %   "rxhttpclient-java"        % "0.5.2"

  val slf4j             = "org.slf4j"                       %   "slf4j-api"               % "1.7.6"


  val mainDependencies = Seq(
    logback
  )

  val mainScalaTestDependencies = Seq (
    scalaTest,
    scalaCheck,
    junitInterface
  )


}