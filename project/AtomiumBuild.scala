import play.PlayScala
import sbt.Keys._
import sbt._
import Keys._
import play.PlayScala



object AtomiumBuild extends Build
                       with BuildSettings {
  import Dependencies._

  val Name = "atomium"
  val playVersion = "2.3.6"


  //----------------------------------------------------------------
  val javaFormatModuleName = Name + "-format-java"
  lazy val javaFormatModule = Project(
    javaFormatModuleName,
    file("modules/format-java"),
    settings = buildSettings(javaFormatModuleName, javaDependencies)
  )

  //----------------------------------------------------------------
  val formatModuleName = Name + "-format"
  lazy val formatModule = Project(
    formatModuleName,
    file("modules/format"),
    settings = buildSettings(formatModuleName)
  ).dependsOn(javaFormatModule)

  //----------------------------------------------------------------
  val clientScalaModuleName = Name + "-client-scala"
  lazy val clientScalaModule = Project(
    clientScalaModuleName,
    file("modules/client-scala"),
    settings = buildSettings(clientScalaModuleName, clientScalaDependencies)
  ).dependsOn(formatModule)
   .aggregate(formatModule)

  //----------------------------------------------------------------
  val serverModuleName = Name + "-server"
  lazy val serverModule = Project(
    serverModuleName,
    file("modules/server"),
    settings = buildSettings(serverModuleName)
  ).dependsOn(formatModule)



  //----------------------------------------------------------------
  val serverMongoModuleName = Name + "-server-mongo"
  lazy val serverMongoModule = Project(
    serverMongoModuleName ,
    file("modules/server-mongo"),
    settings = buildSettings(serverMongoModuleName) ++ Seq(
      libraryDependencies ++= Seq(
        "org.mongodb" % "mongo-java-driver" % "2.0",
        "org.mongodb" %% "casbah" % "2.6.2"
      )
    )
  ).dependsOn(serverModule)



  //----------------------------------------------------------------
  val serverJdbcModuleName = Name + "-server-jdbc"
  lazy val serverJdbcModule = Project(
    serverJdbcModuleName,
    file("modules/server-jdbc"),
    settings = buildSettings(serverJdbcModuleName) ++ Seq(
      libraryDependencies ++= Seq(
        "com.typesafe.slick" %% "slick" % "2.0.0",
        "com.github.tminglei" % "slick-pg_2.10.3" % "0.5.0-RC1"
      )
    )
  ).dependsOn(serverModule)

  import play.Play.autoImport._

  val serverPlayModuleName = Name + "-server-play"
  lazy val serverPlayModule = Project(
    serverPlayModuleName,
    file("modules/server-play")
  ).enablePlugins(PlayScala).settings(
      libraryDependencies += filters
    ).dependsOn(clientScalaModule, serverModule)

  //----------------------------------------------------------------
  val clientJavaModuleName = Name + "-client-java"
  lazy val clientJavaModule = Project(
    clientJavaModuleName,
    file("modules/client-java"),
    settings = buildSettings(clientJavaModuleName, javaDependencies) ++ Seq(
      libraryDependencies ++= Seq(
        "org.slf4j" % "slf4j-api" % "1.7.6", // to be able to exclude logback from runtime dependencies
        "ch.qos.logback" % "logback-classic" % "1.1.1" % "test", // should  be slf4j only
        "com.typesafe.play" %%  "play-json" % "2.3.0" % "provided", // not needed here
        "org.projectlombok" % "lombok" % "1.14.4" % "provided",
        "org.mockito" % "mockito-core" % "1.9.5" % "test",
        "org.assertj" % "assertj-core" % "1.5.0" % "test",
        "be.eliwan" % "jfaker-mockito" % "0.1" % "test",
        "commons-io" % "commons-io" % "2.4" % "test",
        "org.codehaus.jackson" % "jackson-mapper-asl" % "1.9.3" % "test"
      )
    )
  ).dependsOn(clientScalaModule)
   .aggregate(clientScalaModule)



  //----------------------------------------------------------------
  lazy val main = Project(
    Name,
    file("."),
    settings = buildSettings(Name)
  ).aggregate(formatModule, clientScalaModule, clientJavaModule, serverModule, serverMongoModuleName, serverJdbcModule)
  ).aggregate(formatModule, clientScalaModule, clientJavaModule, serverModule, /*serverMongoModuleName, */serverJdbcModule,
    serverPlayModule)
}
