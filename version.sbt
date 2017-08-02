val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "1.2.0" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
