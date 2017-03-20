site.settings

site.includeScaladoc()

organization in ThisBuild := "be.wegenenverkeer"

//scalaVersion in ThisBuild := (crossScalaVersions  .value.lastOption.getOrElse("2.12.1")

crossScalaVersions := Seq("2.10.4", "2.11.8", "2.12.1")

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
