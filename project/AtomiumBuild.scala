import sbt._



object AtomiumBuild extends Build
                       with BuildSettings {
  
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
    settings = buildSettings(clientScalaModuleName)
  ).dependsOn(formatModule)
   .aggregate(formatModule)
   


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
  ).aggregate(formatModule, clientScalaModule, clientJavaModule)
}
