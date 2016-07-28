val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.8.4" //+ snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
