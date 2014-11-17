package be.vlaanderen.awv.atom

/**
 * Responsible for creating URL's in an Atom feed.
 */
trait UrlBuilder {
  /**
   * Creates a link to a feed page.
   *
   * @param id the page ID
   * @return the URL
   */
  def feedLink(id: Long): Url

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
