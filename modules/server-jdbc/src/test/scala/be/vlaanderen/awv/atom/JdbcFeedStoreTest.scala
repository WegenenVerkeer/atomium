package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.models._
import be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple._
import org.joda.time.{LocalDateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class JdbcFeedStoreTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

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
    override def feedLink(start: Long, count: Int): Url = Url(s"/$start/$count")
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
    feedStore.push(1.toString)
    feedStore.push(2.toString)
    feedStore.push(3.toString)
    feedStore.push(4.toString)

    //validate last feed page = oldest page
    val lastPage = feedStore.getFeed(1, 2).get
    lastPage.title should be (Some("Test"))
    lastPage.updated should be (new LocalDateTime())
    lastPage.selfLink.href should be (Url("/1/2"))
    lastPage.lastLink.map(_.href) should be (Some(Url("/1/2")))
    lastPage.previousLink.map(_.href) should be (Some(Url("/3/2")))
    lastPage.nextLink.map(_.href) should be (None)
    lastPage.entries.size should be (2)
    //check reverse chronological order
    lastPage.entries(0).content.value should be (2.toString)
    lastPage.entries(1).content.value should be (1.toString)

    //validate first feed page = newest page
    val firstPage = feedStore.getFeed(3, 2).get
    firstPage.selfLink.href should be (Url("/3/2"))
    firstPage.lastLink.map(_.href) should be (Some(Url("/1/2")))
    firstPage.previousLink.map(_.href) should be (None)
    firstPage.nextLink.map(_.href) should be (Some(Url("/1/2")))
    firstPage.entries.size should be (2)
    firstPage.entries(0).content.value should be (4.toString)
    firstPage.entries(1).content.value should be (3.toString)

    //head of feed = first page containing newest entries
    val headOfFeed = feedStore.getHeadOfFeed(2).get
    headOfFeed should be (firstPage)

    //non existing page
    val emptyPage = feedStore.getFeed(5, 2) should be (None)

    //push extra element
    feedStore.push(List(5.toString))
    val newFirstPage = feedStore.getFeed(5, 2).get
    newFirstPage.entries.size should be (1)
    val newHeadPage = feedStore.getHeadOfFeed(2).get
    newHeadPage should be(newFirstPage)

  }

}
