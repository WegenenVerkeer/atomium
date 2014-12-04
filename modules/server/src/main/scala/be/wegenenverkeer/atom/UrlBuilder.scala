package be.wegenenverkeer.atom

/**
 * Responsible for creating URL's in an Atom feed.
 */
trait UrlBuilder {
  /**
   * Creates a link to a feed page.
   *
   * @param start the starting entry
   * @param count the number of entries in the page             
   * @return the URL
   */
  def feedLink(start:Long, count: Int): Url

  /**
   * Creates a link to a feed.
   *
   * @return the URL
   */
  def collectionLink: Url

  /**
   * Creates the base URL.
   *
   * @return the URL
   */
  def base: Url
}
