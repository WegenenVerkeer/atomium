val baseVersion    = "2.0.0"
lazy val isRelease = true

lazy val buildNr = sys.env.get("bamboo_buildNumber")


def buildVersionNr(vn: String): String = (buildNr, isRelease) match {
  case (Some(bn), true) => s"$vn-${bn}"
  case (None, true)     => s"$vn"
  case (_, false)       => s"$vn-SNAPSHOT"
}

version in ThisBuild := buildVersionNr(baseVersion)
