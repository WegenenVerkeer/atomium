package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.models.EntryModel
import be.wegenenverkeer.atom.slick.FeedDAL
import org.joda.time.DateTimeUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._

class ManualJdbcFeedStoreTest extends FunSuite
  with FeedStoreTestSupport
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)

  implicit var session: Session = _

  var feedStore: ManualJdbcFeedStore[String] = _
  val ENTRIES_TABLE_NAME = "my_feed_entries"

  val db = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver")
  val dal: FeedDAL = new FeedDAL(H2Driver)
  import dal.driver.simple._

  override protected def beforeEach() = {
    session = db.createSession()
    dal.entriesTableQuery(ENTRIES_TABLE_NAME).ddl.create
    feedStore = createFeedStore
  }

  override protected def afterEach(): Unit = {
    dal.entriesTableQuery(ENTRIES_TABLE_NAME).ddl.drop
    FeedModelRegistry.clear()
  }

  override protected def afterAll() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  def createUrlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org/feeds")
    override def collectionLink: Url = ???
  }

  def createFeedStore = ManualJdbcFeedStore[String](
    dal,
    dal.createJdbcContext,
    feedName = "int_feed",
    title = Some("Test"),
    ENTRIES_TABLE_NAME,
    ser = s => s,
    deser = s => s,
    urlBuilder = createUrlBuilder)

  test("push should store entry") {
    feedStore.push(List("1"))

    dal.entriesTableQuery(ENTRIES_TABLE_NAME).length.run should be(1)
    val entry: EntryModel = dal.entriesTableQuery(ENTRIES_TABLE_NAME).first(session)
    entry.value should be("1")

  }

  test("getFeed returns correct page of the feed") {
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

  test("failed transaction should not push entry onto feed") {
    intercept[Exception] {
      session.withTransaction {
        feedStore.push("1")
        feedStore.push("2")
        throw new Exception("tx failure")
      }
    }

    dal.entriesTableQuery(ENTRIES_TABLE_NAME).length.run should be(0) //entry is not stored

    feedStore.push("3")

    //sequence number 1 and 2 are not used,
    val entries = feedStore.getFeedEntries(0, 2, ascending = true)
    entries.size should be (1)
    entries(0).sequenceNr should be (3)
    feedStore.getFeed(0, 2, forward = true).get.entries.size should be (1)
  }

}
