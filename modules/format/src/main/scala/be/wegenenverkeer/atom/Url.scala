package be.wegenenverkeer.atom

/**
 * A wrapper for an URL that adds a path constructor.
 * @param path url path
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
