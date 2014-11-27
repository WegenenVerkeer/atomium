
import sbt._

object Dependencies {

  val mainDependencies = Seq(
    "com.typesafe"        %%  "scalalogging-slf4j"    % "1.0.1",
    "ch.qos.logback"      %   "logback-classic"       % "1.1.1",
    "joda-time"           %   "joda-time"             % "2.3",
    "com.typesafe.play"   %%  "play-json"             % "2.3.0",
    "com.jsuereth"        %%  "scala-arm"             % "1.3"
  )

  val mainTestDependencies = Seq (
    "org.scalatest"            %% "scalatest"              % "2.2.0"    % "test",
    "com.h2database"           %  "h2"                     % "1.0.60"   % "test",
    "de.leanovate.play-mockws" %% "play-mockws"            % "0.12"     % "test",
    "com.typesafe.play"        %% "play-test"              % "2.3.0"    % "test"
  )

  val clientScalaDependencies = Seq (
    "com.typesafe.play"   %%  "play-ws"             % "2.3.0"
  )

  val javaDependencies = Seq (
    "junit" % "junit" % "4.11" % "test",
    "org.projectlombok" % "lombok" % "1.14.4" % "test",
    "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.3",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.3"
  )
}