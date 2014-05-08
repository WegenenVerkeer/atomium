package be.vlaanderen.awv.atom

case class Feed[T](id: String,
                   base: Url, 
                   title: Option[String], 
                   updated: String, 
                   links: List[Link], 
                   entries: List[Entry[T]]) {

  def firstFeedUrl: Option[Url] = {
    links.find(link => link.rel == Link.firstLink) match {
      case Some(link) => Some(link.href)
      case None => None
    }
  }
}