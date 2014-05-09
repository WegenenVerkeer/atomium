import sbt._
import sbt.Configuration
import sbt.Keys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._
import scala.util.Properties


object AtomJsonBuild extends Build {
  val Organization = "be.vlaanderen.awv"
  val Name = "atom-json"
  val Version = "0.1.0" 
  val ScalaVersion = "2.10.3"


  def scalaTestOptions(config: Configuration) = inConfig(config)(Seq(
    testOptions += Tests.Argument("-F", Properties.envOrElse("SCALED_TIME_SPAN", "1"))
  ))

  lazy val testSettings = Seq(
    libraryDependencies ++= Seq(//TEST DEPENDENCIES
      "org.scalatest" %% "scalatest" % "2.1.5" % "test"
    )
  ) ++ scalaTestOptions(Test) ++ scalaTestOptions(jacoco.Config)

  lazy val projectSettings = Seq(
    organization := Organization,
    name := Name,
    scalaVersion := ScalaVersion,
    parallelExecution := false,
    resolvers +=  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    resolvers += Classpaths.typesafeReleases,
    libraryDependencies ++= {
      Seq(
        "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
        "ch.qos.logback" % "logback-classic" % "1.1.1",
        "joda-time" % "joda-time" % "2.3",
        "com.typesafe.play" %% "play-json" % "2.2.2"
      )
    }
  )



  lazy val project = Project(
    "atom-json",
    file("."),
    settings = Defaults.defaultSettings ++
                        projectSettings ++
                        awvsbtplugin.Plugin.defaultAppSettings ++
                        testSettings ++
                        jacoco.settings
  )
}
