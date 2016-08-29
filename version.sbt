val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "1.0.0-JSR310" + snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
