package be.vlaanderen.awv.atom

trait UrlBuilder {
  def feedLink(id: Long): Url
  def collectionLink: Url
  def base: Url
}
