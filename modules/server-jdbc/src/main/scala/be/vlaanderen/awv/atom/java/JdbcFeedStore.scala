package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, JdbcContext}

class JdbcFeedStore[E](c: JdbcContext, feedName: String, title: String, mapper: ElementMapper[E], urlProvider: UrlBuilder)
  extends FeedStore[E](feedName, title match {
    case null => None
    case _ => Some(title)
  }, urlProvider) {

  override def underlying: be.vlaanderen.awv.atom.AbstractFeedStore[E] = new be.vlaanderen.awv.atom.JdbcFeedStore[E](
    c = c,
    feedName = feedName,
    title = title match {
      case null => None
      case _ => Some(title)
    },
    ser = (e) => mapper.serialize(e),
    deser = (v) => mapper.deserialize(v),
    urlBuilder = urlProvider
  )
}
