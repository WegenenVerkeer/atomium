package be.vlaanderen.awv.atom.format

case class Url(path: String) {

  private def this() = this("")

  def /(additionalPath: String): Url = {
    if (additionalPath.startsWith("/")) Url(s"$path$additionalPath")
    else Url(s"$path/$additionalPath")
  }
}
