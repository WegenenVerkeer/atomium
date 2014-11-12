package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.Url

trait UrlBuilder {
  def feedLink(start:Int, count: Int): Url
  def collectionLink: Url
  def base: Url
}
