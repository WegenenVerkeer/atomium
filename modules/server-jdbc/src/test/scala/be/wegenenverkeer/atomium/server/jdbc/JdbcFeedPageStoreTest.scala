package be.wegenenverkeer.atomium.server.jdbc

import java.sql.{Connection, DriverManager}

import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.FeedStoreTestSupport
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class JdbcFeedPageStoreTest extends FunSuite with FeedStoreTestSupport with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {


  val ENTRIES_TABLE_NAME = "my_feed_entries"

  val driverClass = Class.forName("org.h2.Driver")


  test("push should store an entry") { (feedStore, context) =>
    implicit val ctx = context
    feedStore.push(List("1"))
    val entries = feedStore.getFeedEntries(0, 1, ascending = true)
    entries.size should be(1)
    entries.head.entry.getContent.getValue should be("1")
  }

  /**
   * In this test, we check the behaviour of the feed store in the event of a failure that
   * triggers a rollback on the transaction. Remember that the feed store itself assumes
   * that transactional boundaries are managed at a higher level. The level of the
   * transactional boundaries must be so that the atom feed storage of received coexist
   * within the same transaction as the domain changes these received report.
   */
  test("failed transaction should not push entry onto feed") { (feedStore, context) =>
    implicit val ctx = context
    feedStore.push("1")
    feedStore.push("2")
    feedStore.push("3")
    context.connection.rollback() // Simulation of a rollback caused by some failure.

    val entries1 = feedStore.getFeedEntries(0, 10, ascending = true)
    entries1.size should be(0)

    feedStore.push("4")
    val entries2 = feedStore.getFeedEntries(0, 10, ascending = true)
    entries2.size should be(1)
    entries2.head.sequenceNr should be(4)
  }

  test("getFeed returns correct page of the feed") { (feedStore, context) =>
    implicit val ctx = context
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

  def test(description: String)(block: (PgJdbcFeedStore[String], JdbcContext) => Unit): Unit = {
    val connection: Connection = DriverManager.getConnection("jdbc:h2:mem:test")
    /**
     * By default, each SQL statement in JDBC is executed in its own transaction, unless
     * we set autocommit to false. We require that the software using this feed store is
     * self-managing its transactional boundaries.
     */
    connection.setAutoCommit(false)

    implicit val context = JdbcContext(connection)

    val feedStore = createFeedStore
    feedStore.createEntryTable

    try {
      block(feedStore, context)
    } finally {
      feedStore.dropEntryTable
    }

  }

  /**
   * The Postgres JDBC feed store is currently compatible with H2, so we know we
   * can use it here to run tests on H2.
   */
  def createFeedStore(implicit context:JdbcContext): PgJdbcFeedStore[String] =
    PgJdbcFeedStore[String](
      "test_feed",
      Some("title"),
      ENTRIES_TABLE_NAME,
      identity, identity,
      new Url("http://www.example.org/feeds")
    )


}


