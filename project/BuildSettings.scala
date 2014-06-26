import sbt._
import sbt.Configuration
import sbt.Keys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._
import scala.util.Properties

trait BuildSettings extends Dependencies {

  val Organization = "be.vlaanderen.awv"
  
  val Version = "0.1.0" 
  val ScalaVersion = "2.10.3"
  val ScalaBuildOptions = Seq("-unchecked", "-deprecation", "-feature", "-language:reflectiveCalls")


  def scalaTestOptions(config: Configuration) = inConfig(config)(Seq(
    testOptions += Tests.Argument("-F", Properties.envOrElse("SCALED_TIME_SPAN", "1"))
  ))

  lazy val testSettings = Seq(
    libraryDependencies ++= mainTestDependencies
  ) ++ scalaTestOptions(Test) ++ scalaTestOptions(jacoco.Config)

  def projectSettings(projectName:String) = Seq(
    organization := Organization,
    name := projectName,
    version := Version,
    scalaVersion := ScalaVersion,
    scalacOptions := ScalaBuildOptions,
    parallelExecution := false,
    resolvers +=  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    resolvers += Classpaths.typesafeReleases,
    libraryDependencies ++= mainDependencies
  )

  def buildSettings(projectName:String) = {
    Defaults.defaultSettings ++
      projectSettings(projectName) ++
      awvsbtplugin.Plugin.defaultAppSettings ++ 
      testSettings ++
      jacoco.settings
  }

}