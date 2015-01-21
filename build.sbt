net.virtualvoid.sbt.graph.Plugin.graphSettings

site.settings

site.jekyllSupport()

site.includeScaladoc()

com.typesafe.sbt.site.JekyllSupport.requiredGems := Map(
  "jekyll" -> "2.4.0",
  "liquid" -> "2.6.1"
)

organization in ThisBuild := "be.wegenenverkeer"

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild := Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-language:postfixOps"
)