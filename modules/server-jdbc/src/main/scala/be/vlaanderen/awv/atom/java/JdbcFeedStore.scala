package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, JdbcContext}

class JdbcFeedStore[E](c: JdbcContext, feedName: String, mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E] {
  override def underlying: be.vlaanderen.awv.atom.FeedStore[E] = new be.vlaanderen.awv.atom.JdbcFeedStore[E](
    c = c,
    feedName = feedName,
    ser = (e) => mapper.serialize(e),
    deser = (v) => mapper.deserialize(v),
    urlProvider = urlProvider
  )
}
