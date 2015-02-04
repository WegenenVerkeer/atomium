
import sbt._

object Dependencies {

  val playVersion = "2.3.6"

  // main deps
  val logback           = "ch.qos.logback"              %     "logback-classic"         % "1.1.1"
  val jodaTime          = "joda-time"                   %     "joda-time"               % "2.3"
  val jodaConvert       = "org.joda"                    %     "joda-convert"            % "1.2"
  val playJson          = "com.typesafe.play"           %%    "play-json"               % playVersion
  val playWs            = "com.typesafe.play"           %%    "play-ws"                 % playVersion
  val play              = "com.typesafe.play"           %%    "play"                    % playVersion
  val mongoJavaDriver   = "org.mongodb"                 %     "mongo-java-driver"       % "2.0"
  val casbah            = "org.mongodb"                 %%    "casbah"                  % "2.8.0"
  val slick             = "com.typesafe.slick"          %%    "slick"                   % "2.1.0"
  val slickPostgres     = "com.github.tminglei"         %%    "slick-pg"                % "0.7.0"
  val scalaLogging      = "com.typesafe.scala-logging"  %%    "scala-logging"           % "3.1.0"

  // test deps
  val scalaTest         = "org.scalatest"               %%    "scalatest"               % "2.2.0"        % "test"
  val scalaTestPlay     = "org.scalatestplus"           %%    "play"                    % "1.2.0"        % "test"
  val scalaCheck        = "org.scalacheck"              %%    "scalacheck"              % "1.12.1"       % "test"
  val embededMongo      = "com.github.simplyscala"      %%    "scalatest-embedmongo"    % "0.2.2"        % "test"
  val h2database        = "com.h2database"              %     "h2"                      % "1.0.60"       % "test"
  val playMockWs        = "de.leanovate.play-mockws"    %%    "play-mockws"             % "0.12"         % "test"
  val playTest          = "com.typesafe.play"           %%    "play-test"               % playVersion    % "test"

  // java deps
  val junit             = "junit"                           %   "junit"                   % "4.11"    % "test"
  val lombok            = "org.projectlombok"               %   "lombok"                  % "1.14.4"  % "test"
  val jacksonDatabind   = "com.fasterxml.jackson.core"      %   "jackson-databind"        % "2.4.3"
  val jacksonJoda       = "com.fasterxml.jackson.datatype"  %   "jackson-datatype-joda"   % "2.4.3"


  val slf4j             = "org.slf4j"                       %   "slf4j-api"               % "1.7.6"
  val mockitoCore       = "org.mockito"                     %   "mockito-core"            % "1.9.5"   % "test"
  val assertJ           = "org.assertj"                     %   "assertj-core"            % "1.5.0"   % "test"
  val jfakerMockito     = "be.eliwan"                       %   "jfaker-mockito"          % "0.1"     % "test"
  val commonsIo         = "commons-io"                      %   "commons-io"              % "2.4"     % "test"


  val mainDependencies = Seq(
    logback,
    jodaTime,
	  jodaConvert
  )

  val mainTestDependencies = Seq (
    scalaTest,
    scalaCheck
  )


}