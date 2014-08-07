import sbt._
import Keys._



object AtomiumBuild extends Build
                       with BuildSettings {
  import Dependencies._

  val Name = "atomium"


  //----------------------------------------------------------------
  val formatModuleName = Name + "-format"
  lazy val formatModule = Project(
    formatModuleName,
    file("modules/format"),
    settings = buildSettings(formatModuleName)
  )



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
        "org.mongodb" %% "casbah" % "2.5.0"
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
   


  //----------------------------------------------------------------
  val clientJavaModuleName = Name + "-client-java"
  lazy val clientJavaModule = Project(
    clientJavaModuleName,
    file("modules/client-java"),
    settings = buildSettings(clientJavaModuleName, javaDependencies)
  ).dependsOn(clientScalaModule)
   .aggregate(clientScalaModule)



  //----------------------------------------------------------------
  lazy val main = Project(
    Name,
    file("."),
    settings = buildSettings(Name)
  ).aggregate(formatModule, clientScalaModule, serverModule, serverMongoModuleName, serverJdbcModule)
}
