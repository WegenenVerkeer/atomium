package be.vlaanderen.awv.atom

import com.mongodb.casbah.MongoClient

object MongoSample {

  val db = MongoClient()("atom-demo")

  implicit val context = MongoContext()

  val feedStoreFactory: (String, MongoContext) => FeedStore[Int] = (feedName, context) => new MongoFeedStore[Int](context, db.getCollection(feedName), db.getCollection("feed_info"), null, null, null)
  val feedService = new FeedService[Int, MongoContext]("int_feed", 100, "int_feed", feedStoreFactory)

  feedService.push(1)
}
