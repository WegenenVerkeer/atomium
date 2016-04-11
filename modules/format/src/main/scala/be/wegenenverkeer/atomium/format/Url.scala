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

    val startsWith = additional.startsWith("/")
    val endsWith = path.endsWith("/")

    if(startsWith && endsWith) {
      //one of the slashes must go
      Url(s"$path${additional.drop(1)}")
    } else if( startsWith || endsWith) {
      // only one slash, ok
      Url(s"$path$additional")
    } else {
      // no slashes, add one
      Url(s"$path/$additional")
    }

  }
}