import play.PlayScala
import sbt.Keys._
import sbt._
import play.Play.autoImport._


object AtomiumBuild extends Build
                       with BuildSettings {
  import Dependencies._

  val Name = "atomium"

  javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint")

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
    settings = buildSettings(clientScalaModuleName) ++ Seq (
      publishArtifact in Test := true
    )
  ).dependsOn(formatModule, serverModule)
   .aggregate(formatModule, serverModule)

  
  //----------------------------------------------------------------
  val commonPlayModuleName = Name + "-common-play"
  lazy val commonPlayModule = Project(
    commonPlayModuleName,
    file("modules/common-play"),
    settings = buildSettings(commonPlayModuleName, commonPlayDependencies)
  ).dependsOn(formatModule)
    .aggregate(formatModule)



  //----------------------------------------------------------------
  val clientPlayModuleName = Name + "-client-play"
  lazy val clientPlayModule = Project(
    clientPlayModuleName,
    file("modules/client-play"),
    settings = buildSettings(clientPlayModuleName, clientPlayDependencies)
  ).dependsOn(clientScalaModule, commonPlayModule)
    .aggregate(clientScalaModule, commonPlayModule)




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
      libraryDependencies ++= Seq(mongoJavaDriver, casbah)
    )
  ).dependsOn(serverModule % "test->test;compile->compile")



  //----------------------------------------------------------------
  val serverSlickModuleName = Name + "-server-slick"
  lazy val serverSlickModule = Project(
    serverSlickModuleName,
    file("modules/server-slick"),
    settings = buildSettings(serverSlickModuleName) ++ Seq(
      libraryDependencies ++= Seq(slick, slickPostgres)
    )
  ).dependsOn(serverModule % "test->test;compile->compile")



  //----------------------------------------------------------------
  val serverJdbcModuleName = Name + "-server-jdbc"
  lazy val serverJdbcModule = Project(
    serverJdbcModuleName,
    file("modules/server-jdbc"),
    settings = buildSettings(serverJdbcModuleName)
  ).dependsOn(serverModule % "test->test;compile->compile")



  //----------------------------------------------------------------
  val serverPlayModuleName = Name + "-server-play"
  lazy val serverPlayModule = Project(
    serverPlayModuleName,
    file("modules/server-play"),
    settings = buildSettings(serverPlayModuleName)
  ).enablePlugins(PlayScala)
	.settings(libraryDependencies ++= Seq(filters, scalaTestPlay))
    .dependsOn(clientScalaModule, serverModule % "test->test;compile->compile", commonPlayModule)



  //----------------------------------------------------------------
  val clientJavaModuleName = Name + "-client-java"
  lazy val clientJavaModule = Project(
    clientJavaModuleName,
    file("modules/client-java"),
    settings = buildSettings(clientJavaModuleName, javaDependencies) ++ Seq(
      libraryDependencies ++= Seq(
        slf4j, // to be able to exclude logback from runtime dependencies
        logback, // should  be slf4j only
        mockitoCore,
        assertJ,
        jfakerMockito,
        commonsIo
      )
    )
  ).dependsOn(clientScalaModule)
   .aggregate(clientScalaModule)



  //----------------------------------------------------------------
  lazy val main = Project(
    Name,
    file("."),
    settings = buildSettings(Name)
  ).aggregate(
      javaFormatModule,
      formatModule,
      commonPlayModule,
      clientScalaModule,
      clientJavaModule,
      serverModule,
      serverMongoModule,
      serverSlickModule,
      serverJdbcModule,
      serverPlayModule
    )
}
