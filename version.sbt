val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "1.3.1" // + snapshotSuffix
isSnapshot := version.value.endsWith(snapshotSuffix)
