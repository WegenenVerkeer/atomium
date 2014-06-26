package be.vlaanderen.awv.atom

case class Feed[T](id: String,
                   base: Url, 
                   title: Option[String], 
                   updated: String, 
                   links: List[Link], 
                   entries: List[Entry[T]]) {

  assert(links.exists(_.rel == Link.selfLink), "Link to self is mandatory")

  val selfLink = buildUrl(Link.selfLink).get // safe, since invariant is check in constructor


  def firstFeedUrl: Option[Url] = {
    links.find(link => link.rel == Link.firstLink) match {
      case Some(link) => Some(link.href)
      case None => None
    }
  }

  def nextLink : Option[String] = buildUrl(Link.nextLink)

  private def buildUrl(linkName:String) : Option[String] = {
    links.collectFirst {
      case Link(`linkName`, url) => url.path
    }
  }
}