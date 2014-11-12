package be.vlaanderen.awv.atom.format

/**
 * A wrapper for an URL that adds a path constructor.
 * @param path
 */
case class Url(path: String) {

  /**
   * Creates a new URL by adding the given path.
   *
   * @param additionalPath the path to be added
   * @return the new URL
   */
  def /(additionalPath: String): Url = {
    if (additionalPath.startsWith("/")) Url(s"$path$additionalPath")
    else Url(s"$path/$additionalPath")
  }
}