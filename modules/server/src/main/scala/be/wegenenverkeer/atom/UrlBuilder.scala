package be.wegenenverkeer.atom

/**
 * Responsible for creating URL's in an Atom feed.
 */
trait UrlBuilder {
  /**
   * Creates a link to a feed page.
   *
   * @param startId the starting entry's id (non inclusive)
   * @param count the number of entries in the page
   * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
   *                else navigate to 'next' elements in feed (towards last page of feed)
   * @return the URL
   */
  def feedLink(startId:Long, count: Int, forward: Boolean): Url = {
    val direction = if (forward) "forward" else "backward"
    Url(startId.toString) / direction / count.toString
  }


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
