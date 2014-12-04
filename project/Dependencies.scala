
import sbt._

object Dependencies {

  val playVersion = "2.3.0"

  // main deps
  val scalaLogging      = "com.typesafe"            %%    "scalalogging-slf4j"      % "1.0.1"
  val logback           = "ch.qos.logback"          %     "logback-classic"         % "1.1.1"
  val jodaTime          = "joda-time"               %     "joda-time"               % "2.3"
  val playJson          = "com.typesafe.play"       %%    "play-json"               % playVersion
  val playWs            = "com.typesafe.play"       %%    "play-ws"                 % playVersion
  val scalaArm          = "com.jsuereth"            %%    "scala-arm"               % "1.3"
  val mongoJavaDriver   = "org.mongodb"             %     "mongo-java-driver"       % "2.0"
  val casbah            = "org.mongodb"             %%    "casbah"                  % "2.6.2"
  val slick             = "com.typesafe.slick"      %%    "slick"                   % "2.0.0"
  val slickPostgres     = "com.github.tminglei"     %     "slick-pg_2.10.3"         % "0.5.0-RC1"

  // test deps
  val scalaTest     = "org.scalatest"               %%    "scalatest"               % "2.2.0"        % "test"
  val embededMongo  = "com.github.simplyscala"      %%    "scalatest-embedmongo"    % "0.2.2"        % "test"
  val h2database    = "com.h2database"              %     "h2"                      % "1.0.60"       % "test"
  val playMockWs    = "de.leanovate.play-mockws"    %%    "play-mockws"             % "0.12"         % "test"
  val playTest      = "com.typesafe.play"           %%    "play-test"               % playVersion    % "test"

  // java deps
  val junit           = "junit"                           %   "junit"                   % "4.11"    % "test"
  val lombok          = "org.projectlombok"               %   "lombok"                  % "1.14.4"  % "test"
  val jacksonDatabind = "com.fasterxml.jackson.core"      %   "jackson-databind"        % "2.4.3"
  val jacksonJoda     = "com.fasterxml.jackson.datatype"  %   "jackson-datatype-joda"   % "2.4.3"


  val slf4j           = "org.slf4j"                       %   "slf4j-api"               % "1.7.6"
  val mockitoCore     = "org.mockito"                     %   "mockito-core"            % "1.9.5"   % "test"
  val assertJ         = "org.assertj"                     %   "assertj-core"            % "1.5.0"   % "test"
  val jfakerMockito   = "be.eliwan"                       %   "jfaker-mockito"          % "0.1"     % "test"
  val commonsIo       = "commons-io"                      %   "commons-io"              % "2.4"     % "test"


  val mainDependencies = Seq(
    scalaLogging,
    logback,
    jodaTime,
    playJson,
    scalaArm
  )

  val mainTestDependencies = Seq (
    scalaTest,
    embededMongo,
    h2database,
    playMockWs,
    playTest
  )

  val clientScalaDependencies = Seq (
    playWs
  )

  val javaDependencies = Seq (
    junit,
    lombok,
    jacksonDatabind,
    jacksonJoda
  )
}