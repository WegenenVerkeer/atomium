val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.8.1" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
