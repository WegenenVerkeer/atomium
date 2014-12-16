package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.models.{EntryModel, FeedTable}
import be.wegenenverkeer.atom.slick.SlickPostgresDriver.simple._
import org.joda.time.{DateTimeUtils, LocalDateTime}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class JdbcFeedStoreTest extends FunSuite
  with FeedStoreTestSupport
  with Matchers
  with BeforeAndAfterAll
  with BeforeAndAfterEach {

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)

  implicit var session: Session = _

  var feedStore: JdbcFeedStore[String] = _

  override protected def beforeEach() = {
    session = Database.forURL("jdbc:h2:mem:test", driver="org.h2.Driver").createSession()
    FeedTable.ddl.create
    feedStore = createFeedStore
  }

  override protected def afterEach(): Unit = {
    FeedTable.ddl.drop
    feedStore.feedModel.entriesTableQuery.ddl.drop
  }

  override protected def afterAll() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  def createUrlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org/feeds")
    override def collectionLink: Url = ???
  }

  def createFeedStore = new JdbcFeedStore[String](
    JdbcContext(session),
    feedName = "int_feed",
    title = Some("Test"),
    ser = s => s,
    deser = s => s,
    urlBuilder = createUrlBuilder
  )

  test("push should store entry") {
    feedStore.push(List("1"))

    FeedTable.length.run should be(1)
    val feedModel = FeedTable.findByName("int_feed").get
    feedModel.name should be ("int_feed")
    feedModel.title should be (Some("Test"))

    feedModel.entriesTableQuery.length.run should be(1)
    val entry: EntryModel = feedModel.entriesTableQuery.first()
    entry.timestamp should be(new LocalDateTime())
    entry.value should be("1")

    //does not work on H2 :-(
    //MTable.getTables("FEED_ENTRIES_1").list().size should be (1)
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

    FeedTable.length.run should be(1)
    val feedModel = FeedTable.findByName("int_feed").get
    feedModel.entriesTableQuery.length.run should be(0) //entry is not stored

    feedStore.push("3")

    //sequence number 1 and 2 are not used,
    val entries = feedStore.getFeedEntries(0, 2, ascending = true)
    entries.size should be (1)
    entries(0).sequenceNr should be (3)
    feedStore.getFeed(0, 2, forward = true).get.entries.size should be (1)
  }

}
