net.virtualvoid.sbt.graph.Plugin.graphSettings

unidocSettings

site.settings

site.asciidoctorSupport()

site.includeScaladoc()

site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api")

organization in ThisBuild := "be.wegenenverkeer"

scalaVersion in ThisBuild := "2.11.5"

crossScalaVersions := Seq("2.10.4", "2.11.5")

scalacOptions in ThisBuild := Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-language:postfixOps"
)


javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}
