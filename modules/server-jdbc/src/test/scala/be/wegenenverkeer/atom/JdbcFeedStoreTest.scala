package be.wegenenverkeer.atom

import _root_.java.sql.{Connection, DriverManager}

import be.wegenenverkeer.atom.jdbc.PostgresDialect
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfterAll, Matchers, FunSuite}

class JdbcFeedStoreTest extends FunSuite
with FeedStoreTestSupport
with Matchers
with BeforeAndAfterAll
with BeforeAndAfterEach {

  val driverClass = Class.forName("org.h2.Driver")
  val connection: Connection = DriverManager.getConnection("jdbc:h2:mem:test")

  val ENTRIES_TABLE_NAME = "my_feed_entries"

  var feedStore: JdbcFeedStore[String] = _

  override protected def beforeEach() = {
    feedStore = createFeedStore
    feedStore.createTables()
  }

  override protected def afterEach(): Unit = {
    feedStore.dropTables()
  }

  override protected def beforeAll() = {
    /**
     * By default, each SQL statement in JDBC is executed in its own transaction, unless
     * we set autocommit to false. We require that the software using this feed store is
     * self-managing its transactional boundaries.
     */
    connection.setAutoCommit(false)
  }

  test("push should store an entry") {
    feedStore.push(List("1"))
    val entries = feedStore.getFeedEntries(0, 1, ascending = true)
    entries.size should be(1)
    entries(0).entry.content.value should be("1")
  }

  /**
   * In this test, we check the behaviour of the feed store in the event of a failure that
   * triggers a rollback on the transaction. Remember that the feed store itself assumes
   * that transactional boundaries are managed at a higher level. The level of the
   * transactional boundaries must be so that the atom feed storage of entries coexist
   * within the same transaction as the domain changes these entries report.
   */
  test("failed transaction should not push entry onto feed") {
    feedStore.push("1")
    feedStore.push("2")
    feedStore.push("3")
    connection.rollback() // Simulation of a rollback caused by some failure.

    val entries1 = feedStore.getFeedEntries(0, 10, ascending = true)
    entries1.size should be(0)

    feedStore.push("4")
    val entries2 = feedStore.getFeedEntries(0, 10, ascending = true)
    entries2.size should be(1)
    entries2(0).sequenceNr should be(4)
  }

  test("getFeed returns correct page of the feed") {
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

  /**
   * The Postgres JDBC feed store is currently compatible with H2, so we know we
   * can use it here to run tests on H2.
   */
  def createFeedStore =
    PgJdbcFeedStore[String](JdbcContext(connection), "test_feed", Some("title"), ENTRIES_TABLE_NAME, s => s, d => d, createUrlBuilder)

  def createUrlBuilder = new UrlBuilder {

    override def base: Url = Url("http://www.example.org/feeds")

    override def collectionLink: Url = ???
  }

}

case class PgJdbcFeedStore[E](context: JdbcContext,
                                    feedName: String,
                                    title: Option[String],
                                    entryTableName: String,
                                    ser: E => String,
                                    deser: String => E,
                                    urlBuilder: UrlBuilder)
  extends JdbcFeedStore[E](context, feedName, title, entryTableName, ser, deser, urlBuilder)
  with PostgresDialect
