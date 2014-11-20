import sbt._
import sbt.Configuration
import sbt.Keys._
import de.johoop.jacoco4sbt._
import JacocoPlugin._
import scala.util.Properties

trait BuildSettings {

  import Dependencies._
  val Organization = "be.vlaanderen.awv"
  
  val Version = "0.2.0-SNAPSHOT"
  val ScalaVersion = "2.10.3"
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
    organization := Organization,
    name := projectName,
    version := Version,
    scalaVersion := ScalaVersion,
    scalacOptions := ScalaBuildOptions,
    parallelExecution := false,
    resolvers +=  "Local Maven" at Path.userHome.asFile.toURI.toURL + ".m2/repository",
    resolvers += Resolver.typesafeRepo("releases"),
    libraryDependencies ++= mainDependencies ++ extraDependencies
  )


  def buildSettings(projectName:String, extraDependencies:Seq[ModuleID] = Seq()) = {
    Defaults.defaultSettings ++
      projectSettings(projectName, extraDependencies) ++
      awvsbtplugin.Plugin.defaultLibrarySettings ++
      testSettings ++
      jacoco.settings
  }

}