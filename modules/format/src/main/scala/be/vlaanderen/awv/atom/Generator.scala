package be.vlaanderen.awv.atom

case class Generator(text:String, uri:Option[Url] = None, version:Option[String] = None)
