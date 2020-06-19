val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "2.0.0" + snapshotSuffix
isSnapshot := version.value.endsWith(snapshotSuffix)
