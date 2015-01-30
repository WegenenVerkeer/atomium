import sbt._
import sbt.Configuration
import sbt.Keys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._
import scala.util.Properties

trait BuildSettings {

  import Dependencies._

  val ScalaBuildOptions = Seq("-unchecked", "-deprecation", "-feature",
    "-language:reflectiveCalls",
    "-language:implicitConversions",
    "-language:postfixOps")


  def scalaTestOptions(config: Configuration) = inConfig(config)(Seq(
    testOptions += Tests.Argument("-F", Properties.envOrElse("SCALED_TIME_SPAN", "1"))
  ))

  lazy val testSettings = Seq(
    libraryDependencies ++= mainTestDependencies
  ) ++ scalaTestOptions(Test) ++ scalaTestOptions(jacoco.Config)

  
  def projectSettings(projectName:String, extraDependencies:Seq[ModuleID]) = Seq(
    name := projectName,
    parallelExecution := false,
    resolvers +=  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    resolvers += Resolver.typesafeRepo("releases"),
    libraryDependencies ++= mainDependencies ++ extraDependencies
  )

  val publishingCredentials = (for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield
    Seq(Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      username,
      password)
    )).getOrElse(Seq())


  val publishSettings = Seq(
    publishMavenStyle := true,
    pomIncludeRepository := { _ => false},
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomExtra := pomInfo,
    credentials ++= publishingCredentials
  )

  def buildSettings(projectName:String, extraDependencies:Seq[ModuleID] = Seq()) = {
    Defaults.defaultSettings ++
      projectSettings(projectName, extraDependencies) ++
      testSettings ++
      publishSettings ++
      jacoco.settings
  }

  lazy val pomInfo = <url>https://github.com/WegenenVerkeer/atomium</url>
      <licenses>
        <license>
          <name>MIT licencse</name>
          <url>http://opensource.org/licenses/MIT</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:WegenenVerkeer/atomium.git</url>
        <connection>scm:git:git@github.com:WegenenVerkeer/atomium.git</connection>
      </scm>
      <developers>
        <developer>
          <id>AWV</id>
          <name>De ontwikkelaars van AWV</name>
          <url>http://www.wegenenverkeer.be</url>
        </developer>
      </developers>


}