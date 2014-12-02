package be.vlaanderen.awv.atom

import _root_.java.net.ServerSocket

import be.vlaanderen.awv.atom.MongoFeedStore.Keys
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.{DeregisterJodaTimeConversionHelpers, RegisterJodaTimeConversionHelpers}
import com.mongodb.{MongoClient => JavaMongoClient}
import org.joda.time.{LocalDateTime, DateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MongoFeedStoreTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with MongoEmbedDatabase {

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
    override def feedLink(start: Long, pageSize: Int): Url = Url(s"/$start/$pageSize")
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
    val feedStore = createFeedStore

    feedStore.push(Seq(1, 2, 3, 4))

    //validate last feed page = oldest page
    val lastPage = feedStore.getFeed(1, 2).get
    lastPage.title should be (None)
    lastPage.updated should be (new LocalDateTime())
    lastPage.selfLink.href should be (Url("/1/2"))
    lastPage.lastLink.map(_.href) should be (Some(Url("/1/2")))
    lastPage.previousLink.map(_.href) should be (Some(Url("/3/2")))
    lastPage.nextLink.map(_.href) should be (None)
    lastPage.entries.size should be (2)
    lastPage.complete() should be (true)
    //check reverse chronological order
    lastPage.entries(0).content.value should be (2)
    lastPage.entries(1).content.value should be (1)

    //validate first feed page = newest page
    val firstPage = feedStore.getFeed(3, 2).get
    firstPage.selfLink.href should be (Url("/3/2"))
    firstPage.lastLink.map(_.href) should be (Some(Url("/1/2")))
    firstPage.previousLink.map(_.href) should be (None)
    firstPage.nextLink.map(_.href) should be (Some(Url("/1/2")))
    firstPage.entries.size should be (2)
    firstPage.complete() should be (false)
    firstPage.entries(0).content.value should be (4)
    firstPage.entries(1).content.value should be (3)

    //head of feed = first page containing newest entries
    val headOfFeed = feedStore.getHeadOfFeed(2).get
    headOfFeed should be (firstPage)

    //non existing page
    val emptyPage = feedStore.getFeed(5, 2) should be (None)

    //push extra element
    feedStore.push(5)
    val newFirstPage = feedStore.getFeed(5, 2).get
    newFirstPage.entries.size should be (1)
    newFirstPage.complete() should be (false)
    val newHeadPage = feedStore.getHeadOfFeed(2).get
    newHeadPage should be(newFirstPage)

  }

  object FreePort {

    def apply(maxRetries: Int = 50) = {
      val s = new ServerSocket(0)
      try { s.getLocalPort } finally { s.close() }
    }
  }


}
