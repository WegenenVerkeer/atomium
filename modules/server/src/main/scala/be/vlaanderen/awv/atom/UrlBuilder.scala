package be.vlaanderen.awv.atom

trait UrlBuilder {
  def feedLink(start:Int, count: Int): Url
  def collectionLink: Url
  def base: Url
}
