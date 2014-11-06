package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, JdbcContext}

class JdbcFeedStore[E](c: JdbcContext, feedName: String, title: String, mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E] {
  override def underlying: be.vlaanderen.awv.atom.FeedStore[E] = new be.vlaanderen.awv.atom.JdbcFeedStore[E](
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
