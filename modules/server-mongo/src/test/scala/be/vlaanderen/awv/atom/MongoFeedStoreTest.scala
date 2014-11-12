package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.Url
import com.mongodb.casbah.commons.conversions.scala.{DeregisterJodaTimeConversionHelpers, RegisterJodaTimeConversionHelpers}
import com.mongodb.{MongoClient => JavaMongoClient}
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, FunSuite}

class MongoFeedStoreTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  RegisterJodaTimeConversionHelpers()

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)

  override protected def afterAll(): Unit = {
    DeregisterJodaTimeConversionHelpers()
    DateTimeUtils.setCurrentMillisSystem()
  }

  override protected def afterEach() = {
    MongoClient()("atom-test")("int_feed") remove(MongoDBObject.empty)
    MongoClient()("atom-test")("feed_info") remove(MongoDBObject.empty)
  }

  def createUrlBuilder = new UrlBuilder {
    override def base: Url = Url("/")
    override def feedLink(id: Long): Url = Url(s"/$id")
    override def collectionLink: Url = ???
  }
  
  def createFeedStore = new MongoFeedStore[Int](
    MongoContext(new JavaMongoClient().getDB("atom-test")),
    collectionName = "int_feed",
    feedInfoCollectionName = "feed_info",
    ser = i => MongoDBObject("value" -> i),
    deser = dbo => dbo.as[Int]("value"),
    urlProvider = createUrlBuilder
  )

  test("update should update feed collection") {
    val feedStore = createFeedStore
    val updates = List(FeedUpdateInfo(
      page = 1,
      title = "Test",
      updated = new DateTime(),
      isNew = true,
      newElements = List(1),
      first = 1,
      previous = None,
      next = None
    ))
    val feedInfo = FeedInfo(
      count = 1,
      lastPage = 1
    )
    feedStore.update(updates, feedInfo)

    val page = MongoClient()("atom-test")("int_feed").underlying.findOne(MongoDBObject("page" -> 1))
    page.as[Int]("page") should be (1)
    page.as[String]("title") should be ("Test")
    page.as[DateTime]("updated") should be(new DateTime())
    page.as[Int]("first") should be (1)
    page.getAs[Int]("next") should be (None)
    page.getAs[Int]("previous") should be (None)

    val elements = page.as[MongoDBList]("elements")
    elements.length should be(1)

    val element = elements.get(0).asInstanceOf[DBObject]
    element.as[Int]("value") should be (1)

    val feedInfoDbo = MongoClient()("atom-test")("feed_info").underlying.findOne(MongoDBObject("feed" -> "int_feed"))
    feedInfoDbo.as[String]("feed") should be("int_feed")
    feedInfoDbo.as[Int]("last_page") should be(1)
    feedInfoDbo.as[Int]("count") should be(1)
  }

  test("getFeed returns a page of the feed") {
    val feedStore = createFeedStore

    val feedCollection = MongoClient()("atom-test")("int_feed")

    feedCollection.insert(MongoDBObject(
      "page" -> 2L,
      "title" -> "Test",
      "updated" -> new DateTime(),
      "first" -> 1L,
      "next" -> 3L,
      "previous" -> 1L,
      "elements" -> MongoDBList(MongoDBObject("value" -> 1))
    ), WriteConcern.Safe)

    val feedInfoCollection = MongoClient()("atom-test")("feed_info")

    feedInfoCollection.insert(MongoDBObject(
      "feed" -> "int_feed",
      "count" -> 1,
      "last_page" -> 3L
    ), WriteConcern.Safe)

    val feedPage = feedStore.getFeed(2).get

    feedPage.id should be ("2")
    feedPage.base should be (Url("/"))
    feedPage.title should be (Some("Test"))
    feedPage.updated should be ((new DateTime()).toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))
    feedPage.selfLink.href should be (Url("/2"))
    feedPage.firstLink.map(_.href) should be (Some(Url("/1")))
    feedPage.previousLink.map(_.href) should be (Some(Url("/1")))
    feedPage.nextLink.map(_.href) should be (Some(Url("/3")))

    feedPage.entries.size should be (1)
    val entry = feedPage.entries(0)
    entry.content.value should be (List(1))
  }
}
