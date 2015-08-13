val snapshotSuffix = "-SNAPSHOT"

version in ThisBuild := "0.5.0" 

isSnapshot := version.value.endsWith(snapshotSuffix)