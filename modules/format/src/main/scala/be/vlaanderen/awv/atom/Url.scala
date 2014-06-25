package be.vlaanderen.awv.atom

case class Url(path: String) {

  def /(additionalPath: String): Url = {
    if (additionalPath.startsWith("/")) Url(s"$path$additionalPath")
    else Url(s"$path/$additionalPath")
  }
}