package be.wegenenverkeer.atomium.japi.server.mongo

import be.wegenenverkeer.atomium.japi.server.FeedStore
import be.wegenenverkeer.atomium.server.{AbstractFeedStore, UrlBuilder}
import be.wegenenverkeer.atomium.server.mongo.MongoContext
import be.wegenenverkeer.atomium.server.mongo

class MongoFeedStore[E](c: MongoContext,
                        feedName: String,
                        title: Option[String],
                        feedEntriesCollectionName: Option[String],
                        feedInfoCollectionName: String,
                        mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E](feedName, title, urlProvider) {

  override def underlying: AbstractFeedStore[E] = new mongo.MongoFeedStore[E](
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
