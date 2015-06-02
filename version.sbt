val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.4.0"

isSnapshot := version.value.endsWith(snapshotSuffix)