import de.johoop.jacoco4sbt.JacocoPlugin._
import sbt.Keys._
import sbt.{Configuration, _}

import scala.util.Properties

trait BuildSettings {

  import Dependencies._

  val projectName = "atomium"

  //TODO -- why do we need this? Enabling this means only ScalaTest is run, and not JUNIT. Unacceptable.
  // Re-enable in modules that really need it
  def scalaTestOptions(config: Configuration) = inConfig(config)(Seq(
    testOptions += Tests.Argument("-F", Properties.envOrElse("SCALED_TIME_SPAN", "1"))
  ))

  lazy val testSettings = Seq(
    libraryDependencies ++= mainTestDependencies
  )


  def project(moduleName: String): Project = {
    Project(
      id = projectName + "-" + moduleName,
      base = file("modules/" + moduleName),
      settings = projectSettings()
    )
  }


  def mainProject(modules: ProjectReference*): Project = {
    Project(
      id = projectName,
      base = file("."),
      settings = projectSettings()
    ).settings(publishArtifact := false)
      .aggregate(modules: _*)
  }

  private def projectSettings() = {

    val projectSettings = Seq(
      parallelExecution := false,
      resolvers += "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
      resolvers += Resolver.typesafeRepo("releases"),
      libraryDependencies ++= mainDependencies
    )

    Defaults.coreDefaultSettings ++ projectSettings ++ testSettings ++ publishSettings ++ jacoco.settings
  }

  val publishSettings = {

    val publishingCredentials = {

      val credentials =
        for {
          username <- Option(System.getenv().get("SONATYPE_USERNAME"))
          password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
        } yield
          Credentials(
            "Sonatype Nexus Repository Manager",
            "oss.sonatype.org",
            username,
            password
          )

      credentials.toSeq
    }

    // explicitly creating a Def to avoid cluttered code inside Seq definition
    val repoToPublish = Def setting {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }


    val pomInfo = {
      <url>https://github.com/WegenenVerkeer/atomium</url>
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

    Seq(
      publishMavenStyle := true,
      pomIncludeRepository := { _ => false},
      publishTo := repoToPublish.value,
      pomExtra := pomInfo,
      credentials ++= publishingCredentials
    )
  }


}