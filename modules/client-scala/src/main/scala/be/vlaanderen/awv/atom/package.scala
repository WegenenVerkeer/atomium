package be.vlaanderen.awv

package object atom {

  implicit class FeedExtension[E](feed:Feed[E]) {

    def selfLink : String = buildUrl(Link.selfLink).get // self link moet er altijd zijn, anders kunnen we niks doen

    def nextLink : Option[String] = buildUrl(Link.nextLink)

    private def buildUrl(linkName:String) : Option[String] = {
      feed.links.collectFirst {
        case Link(`linkName`, url) => url.path
      }
    }
  }
}
