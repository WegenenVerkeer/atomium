val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "1.3.0" //+ snapshotSuffix

isSnapshot := version.value.endsWith(snapshotSuffix)
