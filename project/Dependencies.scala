import sbt._

object Dependencies {

  val jacksonVersion = "2.22.1"
  val logbackVersion = "1.5.38"
  val slf4jVersion   = "2.0.18"

  // main deps
  // logback is a logging *backend*: only needed to run our own tests, never forced on consumers.
  val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % "test"

  // JaxbCodec needs JAXB only; the api carries the annotations we expose, the runtime backs
  // JAXBContext.newInstance. (This replaces jaxws-rt, a full SOAP stack we never used.)
  val jaxbApi     = "jakarta.xml.bind"   % "jakarta.xml.bind-api" % "4.0.5"
  val jaxbRuntime = "org.glassfish.jaxb" % "jaxb-runtime"         % "4.0.9" % "runtime"

  // test deps
  val wiremock              = "com.github.tomakehurst" % "wiremock" % "2.26.3" % "test"
  val postgresdriver        = "org.postgresql" % "postgresql" % "42.2.13" % "test"
  val testcontainersVersion = "1.20.4"
  val testcontainers        = "org.testcontainers" % "testcontainers" % testcontainersVersion % "test"
  val testcontainersJunit   = "org.testcontainers" % "junit-jupiter" % testcontainersVersion % "test"
  val testcontainersPsql    = "org.testcontainers" % "postgresql" % testcontainersVersion % "test"

  // java deps
  val junit           = "junit"                      % "junit"            % "4.11" % "test"
  val junitInterface  = "com.novocode"               % "junit-interface"  % "0.11" % "test->default"
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
  val rxhttpclient    = "be.wegenenverkeer"          % "rxhttpclient"     % "2.0.1"
  // reactor-adapter 3.4+ is required: RxJava 3.1 moved QueueSubscription out of the internal.fuseable package.
  val reactorVersion = "3.5.20"
  val reactor        = "io.projectreactor"        % "reactor-core"    % reactorVersion % "test"
  val reactorTest    = "io.projectreactor"        % "reactor-test"    % reactorVersion % "test"
  val reactorAdapter = "io.projectreactor.addons" % "reactor-adapter" % "3.5.5"        % "test"

  val slf4j = "org.slf4j" % "slf4j-api" % slf4jVersion

  val mainDependencies = Seq(
    logback,
    jaxbApi,
    jaxbRuntime,
    testcontainers,
    testcontainersJunit,
    testcontainersPsql
  )

}
