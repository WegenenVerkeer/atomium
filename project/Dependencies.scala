
import sbt._

object Dependencies {
  val play25Version = "2.5.4"
  val play26Version = "2.6.2"

  // main deps
  val logback           = "ch.qos.logback"              %     "logback-classic"               % "1.2.3"
  val rxscala           = "io.reactivex"                %%    "rxscala"                       % "0.26.5"
  val jaxwsRt           = "com.sun.xml.ws"              %     "jaxws-rt"                      % "2.3.2-1" pomOnly()

  //play25 deps
//  val play25Json          = "com.typesafe.play"           %%    "play-json"                     % play25Version
//  val play25              = "com.typesafe.play"           %%    "play"                          % play25Version
//  val play25Test          = "com.typesafe.play"           %%    "play-test"                     % play25Version    % "test"

  //play26 deps
  val play26Json          = "com.typesafe.play"           %%    "play-json"                     % play26Version
  val play26              = "com.typesafe.play"           %%    "play"                          % play26Version
  val play26Test          = "com.typesafe.play"           %%    "play-test"                     % play26Version    % "test"

  // test deps
  val scalaTest         = "org.scalatest"               %%    "scalatest"               % "3.1.2"        % "test"
  val scalaTestPlay26   = "org.scalatestplus.play"      %%    "scalatestplus-play"      % "3.1.1"        % "test"
  val scalaCheck        = "org.scalacheck"              %%    "scalacheck"              % "1.14.1"       % "test"
  val wiremock          = "com.github.tomakehurst"      %     "wiremock"                % "2.26.3"         % "test"
  val postgresdriver    = "org.postgresql"              %     "postgresql"              % "42.2.13" % "test"

  // java deps
  val junit             = "junit"                           %   "junit"                    % "4.11"    % "test"
  val junitInterface    = "com.novocode"                    %   "junit-interface"          % "0.11"    % "test->default"
  val jacksonDatabind   = "com.fasterxml.jackson.core"      %   "jackson-databind"         % "2.4.3"
  val rxhttpclient      = "be.wegenenverkeer"               %   "rxhttpclient"             % "2.0-RC2"

  val slf4j             = "org.slf4j"                       %   "slf4j-api"               % "1.7.30"


  val mainDependencies = Seq(
    logback,
    jaxwsRt
  )

  val mainScalaTestDependencies = Seq (
    scalaTest,
    scalaCheck,
    junitInterface
  )
}