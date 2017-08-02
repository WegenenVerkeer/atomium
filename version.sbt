val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "1.1.0" //+ snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
