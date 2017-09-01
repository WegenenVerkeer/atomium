
import sbt._

object Dependencies {

  val playVersion = "2.4.2"
  val play25Version = "2.5.4"
  val play26Version = "2.6.2"

  // main deps
  val logback           = "ch.qos.logback"              %     "logback-classic"               % "1.1.1"
  val rxscala           = "io.reactivex"                %%    "rxscala"                       % "0.26.5"
  val playJson          = "com.typesafe.play"           %%    "play-json"                     % playVersion
  val playWs            = "com.typesafe.play"           %%    "play-ws"                       % playVersion
  val play              = "com.typesafe.play"           %%    "play"                          % playVersion

  //play25 deps
  val play25Json          = "com.typesafe.play"           %%    "play-json"                     % play25Version
  val play25Ws            = "com.typesafe.play"           %%    "play-ws"                       % play25Version
  val play25              = "com.typesafe.play"           %%    "play"                          % play25Version
  val play25Test          = "com.typesafe.play"           %%    "play-test"                     % play25Version    % "test"

  //play26 deps
  val play26Json          = "com.typesafe.play"           %%    "play-json"                     % play26Version
  val play26Ws            = "com.typesafe.play"           %%    "play-ws"                       % play26Version
  val play26              = "com.typesafe.play"           %%    "play"                          % play26Version
  val play26Test          = "com.typesafe.play"           %%    "play-test"                     % play26Version    % "test"

  // test deps
  val scalaTest         = "org.scalatest"               %%    "scalatest"               % "3.0.1"        % "test"
  val scalaTestPlay     = "org.scalatestplus"           %%    "play"                    % "1.2.0"        % "test"
  val scalaTestPlay26   = "org.scalatestplus.play"      %%    "scalatestplus-play"      % "3.1.1"        % "test"
  val scalaCheck        = "org.scalacheck"              %%    "scalacheck"              % "1.13.4"       % "test"
  val h2database        = "com.h2database"              %     "h2"                      % "1.0.60"       % "test"
  val playMockWs        = "de.leanovate.play-mockws"    %%    "play-mockws"             % "0.12"         % "test"
  val playTest          = "com.typesafe.play"           %%    "play-test"               % playVersion    % "test"
  val wiremock          = "com.github.tomakehurst"      %     "wiremock"                % "1.57"         % "test"
  val postgresdriver    = "org.postgresql"              %     "postgresql"              % "9.4-1200-jdbc41" % "test"

  // java deps
  val junit             = "junit"                           %   "junit"                    % "4.11"    % "test"
  val junitInterface    = "com.novocode"                    %   "junit-interface"          % "0.11"    % "test->default"
  val jacksonDatabind   = "com.fasterxml.jackson.core"      %   "jackson-databind"         % "2.4.3"
  val rxhttpclient      = "be.wegenenverkeer"               %   "rxhttpclient-java"        % "0.4.0"
  val lombok            = "org.projectlombok"               %   "lombok"                   % "1.16.10" % "provided"
  val springContext     = "org.springframework"             %   "spring-context"           % "4.2.0.RELEASE"
  val springTx          = "org.springframework"             %   "spring-tx"                % "4.2.0.RELEASE"
  val jaxRsApi          = "org.jboss.spec.javax.ws.rs"      %   "jboss-jaxrs-api_2.0_spec" % "1.0.0.Final"
  val restEasy          = "org.jboss.resteasy"              %   "resteasy-jaxrs"           % "3.0.19.Final" % "provided"

  val slf4j             = "org.slf4j"                       %   "slf4j-api"               % "1.7.6"
  val mockitoCore       = "org.mockito"                     %   "mockito-core"            % "1.9.5"   % "test"
  val assertJ           = "org.assertj"                     %   "assertj-core"            % "1.5.0"   % "test"
  val commonsIo         = "commons-io"                      %   "commons-io"              % "2.4"     % "test"


  val mainDependencies = Seq(
    logback
  )

  val mainScalaTestDependencies = Seq (
    scalaTest,
    scalaCheck,
    junitInterface
  )


}