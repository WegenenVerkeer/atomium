val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.6.0"

isSnapshot := version.value.endsWith(snapshotSuffix)