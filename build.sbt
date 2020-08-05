site.settings

site.includeScaladoc()

organization in ThisBuild := "be.wegenenverkeer"

crossScalaVersions := Seq("2.12.8", "2.13.2")

scalacOptions in ThisBuild := Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-language:reflectiveCalls",
  "-language:implicitConversions",
  "-language:postfixOps"
)


javacOptions ++= Seq("-source", "11", "-target", "11", "-Xlint")

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "11")
    sys.error("Java 11 is required for this project.")
}
