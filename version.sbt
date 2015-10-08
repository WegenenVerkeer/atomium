val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.6.1" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)