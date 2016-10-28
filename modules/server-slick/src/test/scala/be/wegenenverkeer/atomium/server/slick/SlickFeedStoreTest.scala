package be.wegenenverkeer.atomium.server.slick

import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.FeedStoreTestSupport
import be.wegenenverkeer.atomium.server.slick.models.EntryModel
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.slick.driver.H2Driver

class SlickFeedStoreTest extends FunSuite with FeedStoreTestSupport with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val timeMillis = System.currentTimeMillis()



  val ENTRIES_TABLE_NAME = "my_feed_entries"

  val dal = new FeedDAL(H2Driver)

  import dal.driver.simple._

  def test(description: String)(block: SlickJdbcContext => Unit): Unit = {

    val db = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver")
    implicit val session = db.createSession()

    dal.entriesTableQuery(ENTRIES_TABLE_NAME).ddl.create

    block(dal.createJdbcContext)

    dal.entriesTableQuery(ENTRIES_TABLE_NAME).ddl.drop
  }


  def createFeedStore(implicit context: SlickJdbcContext) = {
    SlickFeedStore[String](
      dal,
      feedName = "int_feed",
      title = Some("Test"),
      ENTRIES_TABLE_NAME,
      ser = s => s,
      deser = s => s,
      url = new Url("http://www.example.org/feeds")
    )
  }

  test("push should store entry") { implicit context =>
    implicit val session = context.session
    val feedStore = createFeedStore
    feedStore.push(List("1"))

    dal.entriesTableQuery(ENTRIES_TABLE_NAME).length.run should be(1)
    val entry: EntryModel = dal.entriesTableQuery(ENTRIES_TABLE_NAME).first(session)
    entry.value should be("1")

  }

  test("getFeed returns correct page of the feed") { implicit context =>
    val feedStore = createFeedStore
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

  test("failed transaction should not push entry onto feed") { implicit context =>
    implicit val session = context.session
    val feedStore = createFeedStore

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
    entries.size should be(1)
    entries(0).sequenceNr should be(3)
    feedStore.getFeed(0, 2, forward = true).get.getEntries.size should be(1)
  }

}
