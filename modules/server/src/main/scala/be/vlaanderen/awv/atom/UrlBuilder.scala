package be.vlaanderen.awv.atom

/**
 * Responsible for creating URL's in an Atom feed.
 */
trait UrlBuilder {

  def feedLink(start:Int, count: Int): Url

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
