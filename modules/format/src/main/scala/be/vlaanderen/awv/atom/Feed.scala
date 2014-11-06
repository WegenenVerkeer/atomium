package be.vlaanderen.awv.atom

case class Feed[T](base: Url,
                   title: Option[String], 
                   updated: String, 
                   links: List[Link], 
                   entries: List[Entry[T]]) {

  assert(links.exists(_.rel == Link.selfLink), "Link to self is mandatory")

  val selfLink : Link = findLinkByName(Link.selfLink).get // safe, since invariant is checked in constructor
  val nextLink : Option[Link] = findLinkByName(Link.nextLink)
  val firstLink : Option[Link] = findLinkByName(Link.firstLink)
  val previousLink : Option[Link] = findLinkByName(Link.previousLink)
  val lastLink : Option[Link] = findLinkByName(Link.lastLink)
  val collectionLink : Option[Link] = findLinkByName(Link.collectionLink)


  def findLinkByName(linkName:String) : Option[Link] = {
    links.collectFirst {
      case link @ Link(`linkName`, _) => link
    }
  }
}