package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, MongoContext}

class MongoFeedStore[E](c: MongoContext, collectionName: String, feedInfoCollectionName: String, mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E] {
  override def underlying: be.vlaanderen.awv.atom.FeedStore[E] = new be.vlaanderen.awv.atom.MongoFeedStore[E](
    c = c,
    collectionName = collectionName,
    feedInfoCollectionName = feedInfoCollectionName,
    ser = (e) => mapper.serialize(e),
    deser = (dbo) => mapper.deserialize(dbo),
    urlProvider = urlProvider
  )
}
