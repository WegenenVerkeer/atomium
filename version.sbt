val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.8.5" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
