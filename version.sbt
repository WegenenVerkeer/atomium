val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.8.2" //+ snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
