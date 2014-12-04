package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.{AbstractFeedStore, MongoContext, UrlBuilder}

class MongoFeedStore[E](c: MongoContext,
                        feedName: String,
                        title: Option[String],
                        feedEntriesCollectionName: Option[String],
                        feedInfoCollectionName: String,
                        mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E](feedName, title, urlProvider) {

  override def underlying: AbstractFeedStore[E] = new be.wegenenverkeer.atom.MongoFeedStore[E](
    c = c,
    feedName,
    title,
    feedEntriesCollectionName = feedEntriesCollectionName,
    feedInfoCollectionName = feedInfoCollectionName,
    ser = (e) => mapper.serialize(e),
    deser = (dbo) => mapper.deserialize(dbo),
    urlProvider = urlProvider
  )


}
