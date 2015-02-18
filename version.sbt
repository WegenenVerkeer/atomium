val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.3.0"

isSnapshot := version.value.endsWith(snapshotSuffix)