import sbt._
import Keys._
import xerial.sbt.Sonatype.autoImport.sonatypePublishToBundle

object PublishingSettings {

  lazy val publishingSettings = if (isBambooBuild) AWVPublishSettings else OSSRHPublishSettings

  lazy val isBambooBuild = sys.env.get("bamboo_buildNumber").isDefined //if so, then assume it is being built in AWV environment


  lazy val AWVPublishSettings = {
    Seq(
      publishTo  := {
        val awvRepo = "https://collab.mow.vlaanderen.be/artifacts/repository/"
        if (isSnapshot.value) {
          Some("collab snapshots" at awvRepo + "maven-snapshots")
        } else {
          Some("collab releases" at awvRepo + "maven-releases")
        }
      },
      publishMavenStyle := true
    )
  }

  lazy val OSSRHPublishSettings = {

    val publishingCredentials = {

      val credentials =
        for {
          username <- Option(System.getenv().get("SONATYPE_USERNAME"))
          password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
        } yield Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          username,
          password
        )

      credentials.toSeq
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
      publishTo := sonatypePublishToBundle.value,
      pomIncludeRepository := { _ =>
        false
      },
      pomExtra := pomInfo,
      credentials ++= publishingCredentials
    )
  }



}
