
import sbt._

trait Dependencies {

  val mainDependencies = Seq(
    "com.typesafe"        %%  "scalalogging-slf4j"    % "1.0.1",
    "ch.qos.logback"      %   "logback-classic"       % "1.1.1",
    "joda-time"           %   "joda-time"             % "2.3",
    "com.typesafe.play"   %%  "play-json"             % "2.2.2",
    "org.scalaz"          %%  "scalaz-core"           % "7.0.5"
  )

  val mainTestDependencies = Seq (
    "org.scalatest"           %% "scalatest"              % "2.0"    % "test"
  )
}