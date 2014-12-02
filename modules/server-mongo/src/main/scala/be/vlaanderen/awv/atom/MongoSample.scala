package be.vlaanderen.awv.atom

import com.mongodb.casbah.MongoClient

object MongoSample {
  implicit val context = MongoContext(MongoClient()("atom-demo").underlying)

//  val feedStoreFactory: (String, MongoContext) => FeedStore[Int] = (feedName, context) => new MongoFeedStore[Int](context, feedName, "feed_info", null, null, null)
//  val feedService = new FeedService[Int, MongoContext]("int_feed", 100, feedStoreFactory)
//
//  feedService.push(1)
}
