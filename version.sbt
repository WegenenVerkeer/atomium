val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.8.3" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
