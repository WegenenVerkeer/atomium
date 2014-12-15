package be.wegenenverkeer.atom

import _root_.java.net.ServerSocket

import be.wegenenverkeer.atom.MongoFeedStore.Keys
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.{DeregisterJodaTimeConversionHelpers, RegisterJodaTimeConversionHelpers}
import com.mongodb.{MongoClient => JavaMongoClient}
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MongoFeedStoreTest extends FunSuite with Matchers with BeforeAndAfterAll
  with BeforeAndAfterEach with MongoEmbedDatabase with FeedStoreTestSupport {

  RegisterJodaTimeConversionHelpers()

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)
  val port = FreePort()
  val mongoProps: MongodProps = mongoStart(port = port)

  def createMongoClient = MongoClient(host = "localhost", port = port)
  def createJavaMongoClient = new JavaMongoClient("localhost", port)

  override protected def afterAll(): Unit = {
    mongoStop(mongoProps)
    DeregisterJodaTimeConversionHelpers()
    DateTimeUtils.setCurrentMillisSystem()
  }

  override protected def afterEach() = {
    createMongoClient("atom-test")("int_feed") remove MongoDBObject.empty
    createMongoClient("atom-test")("feed_info") remove MongoDBObject.empty
  }

  def createUrlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org")
    override def collectionLink: Url = ???
  }
  
  def createFeedStore = new MongoFeedStore[Int](
    MongoContext(createJavaMongoClient.getDB("atom-test")),
    feedName = "int_feed",
    feedInfoCollectionName = "feed_info",
    ser = i => MongoDBObject("value" -> i),
    deser = dbo => dbo.as[Int]("value"),
    urlProvider = createUrlBuilder
  )

  test("feed store creation should insert into feedInfoCollection") {
    val feedStore = createFeedStore
    val feedInfoCollection = createMongoClient("atom-test")("feed_info")
    feedInfoCollection.size should be (1)
    val feedObject = feedInfoCollection.find().toList.head
    feedObject.as[String](Keys._Id) should be ("int_feed")
    feedObject.as[Int](Keys.Sequence) should be (0)
  }

  test("push should store entries") {
    val feedStore = createFeedStore

    //push first value on feedstore
    feedStore.push(666)

    val feedInfoCollection = createMongoClient("atom-test")("feed_info")
    val feeds = feedInfoCollection.find().toList
    feeds.size should be(1)
    var feedObject = feeds.head
    feedObject.as[String](Keys._Id) should be ("int_feed")
    feedObject.as[Int](Keys.Sequence) should be (1) //sequence is incremented

    val feedEntriesCollection = createMongoClient("atom-test")("int_feed")
    feedEntriesCollection.size should be (1)
    var entries = feedEntriesCollection.find().toList
    entries.size should be (1)

    val firstObject = entries.head
    firstObject.as[Int](Keys._Id) should be (1)
    firstObject.as[DateTime](Keys.Timestamp).toDate.getTime should be (timeMillis)
    firstObject.as[DBObject](Keys.Content).as[Int]("value") should be (666)

    //push second value on feedstore
    feedStore.push(999)

    feedEntriesCollection.size should be (2)
    entries =  feedEntriesCollection.find().toList
    entries.size should be (2)

    val secondObject = entries.tail.head
    secondObject.as[Int](Keys._Id) should be (2)
    secondObject.as[DateTime](Keys.Timestamp).toDate.getTime should be (timeMillis)
    secondObject.as[DBObject](Keys.Content).as[Int]("value") should be (999)

    feedObject = feedInfoCollection.find().toList.head
    feedObject.as[String](Keys._Id) should be ("int_feed")
    feedObject.as[Int](Keys.Sequence) should be (2)
  }

  test("getFeed returns correct page of the feed") {
    testFeedStorePaging(feedStore = new MongoFeedStore[String](
      MongoContext(createJavaMongoClient.getDB("atom-test")),
      feedName = "string_feed_1",
      feedInfoCollectionName = "feed_info",
      ser = i => MongoDBObject("value" -> i),
      deser = dbo => dbo.as[String]("value"),
      urlProvider = createUrlBuilder
    ))
  }

  object FreePort {

    def apply(maxRetries: Int = 50) = {
      val s = new ServerSocket(0)
      try { s.getLocalPort } finally { s.close() }
    }
  }


}
