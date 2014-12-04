package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.{AbstractFeedStore, JdbcContext, UrlBuilder}

class JdbcFeedStore[E](c: JdbcContext, feedName: String, title: String, mapper: ElementMapper[E], urlProvider: UrlBuilder)
  extends FeedStore[E](feedName, Option(title), urlProvider) {

  override def underlying: AbstractFeedStore[E] = new be.wegenenverkeer.atom.JdbcFeedStore[E](
    c = c,
    feedName = feedName,
    title = Option(title),
    ser = (e) => mapper.serialize(e),
    deser = (v) => mapper.deserialize(v),
    urlBuilder = urlProvider
  )
}
