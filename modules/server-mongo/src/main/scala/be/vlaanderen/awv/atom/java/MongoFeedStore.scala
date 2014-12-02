package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, MongoContext}

class MongoFeedStore[E](c: MongoContext,
                        feedName: String,
                        title: Option[String],
                        feedEntriesCollectionName: Option[String],
                        feedInfoCollectionName: String,
                        mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E](feedName, title, urlProvider) {

  override def underlying: be.vlaanderen.awv.atom.AbstractFeedStore[E] = new be.vlaanderen.awv.atom.MongoFeedStore[E](
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
