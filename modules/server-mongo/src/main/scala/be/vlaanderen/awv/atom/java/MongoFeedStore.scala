package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{UrlBuilder, MongoContext}
import com.mongodb.DBCollection

class MongoFeedStore[E](context:MongoContext, feedCollection: DBCollection, feedInfoCollection: DBCollection, mapper: ElementMapper[E], urlProvider: UrlBuilder) extends FeedStore[E] {
  override def underlying: be.vlaanderen.awv.atom.FeedStore[E] = new be.vlaanderen.awv.atom.MongoFeedStore[E](
    context = context,
    feedCollection = feedCollection,
    feedInfoCollection = feedInfoCollection,
    ser = (e) => mapper.serialize(e),
    deser = (dbo) => mapper.deserialize(dbo),
    urlProvider = urlProvider
  )
}
