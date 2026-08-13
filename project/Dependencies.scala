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
  // 2.x is required to talk to Docker Engine 29+, which rejects any API version below 1.44.
  // 1.20.4 pulls docker-java 3.4.0 and asks for 1.32; 2.0.5 pulls 3.7.1 and asks for 1.44
  // (verified against a live daemon: it asks for 1.44 even when the daemon offers 1.48, so this
  // is a raised floor, not negotiation, and will need revisiting if Docker raises its own again).
  // The modules are renamed in 2.x.
  val testcontainersVersion = "2.0.5"
  val testcontainers        = "org.testcontainers" % "testcontainers" % testcontainersVersion % "test"
  val testcontainersJunit   = "org.testcontainers" % "testcontainers-junit-jupiter" % testcontainersVersion % "test"
  val testcontainersPsql    = "org.testcontainers" % "testcontainers-postgresql" % testcontainersVersion % "test"

  // java deps
  // 4.13.2, not 4.11: testcontainers 1.x used to pull 4.13.2 in transitively and TimestampFormatTest
  // relies on it (single-argument @Parameters landed in 4.12). 4.13.1 also fixes CVE-2020-15250.
  val junit           = "junit"                      % "junit"            % "4.13.2" % "test"
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
