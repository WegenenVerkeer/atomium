package be.wegenenverkeer.atomium.format

/**
 * A wrapper for an URL that adds a path constructor.
 * @param path url path
 */
case class Url(path: String) {

  /**
   * Creates a new URL by adding the given path Url
   *
   * @param additionalPath the path to be added
   * @return the new URL
   */
  def /(additionalPath: Url) : Url = this / additionalPath.path

  /**
   * Creates a new URL by adding the given path as String
   *
   * @param additionalPath the path to be added
   * @return the new URL
   */
  def /(additionalPath: String): Url = {
    add(additionalPath)
  }

  def /(additionalPath: Long): Url =
    add(additionalPath.toString)

  def /(additionalPath: Int): Url =
    add(additionalPath.toString)

  private def add(additional: String) : Url= {
    val raw = s"$path/$additional"
    def contractRepeatedSlashes: String = {
      val repeatedRegex = "(/)\\1+"
      if (raw.startsWith("http://")) s"http:/${raw.substring(6).replaceAll(repeatedRegex, "$1")}"
      else raw.replaceAll(repeatedRegex, "$1")
    }
    Url(contractRepeatedSlashes)
  }
}