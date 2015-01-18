import be.wegenenverkeer.atom.{MemoryFeedStore, FeedStoreTestSupport, Url, UrlBuilder}
import org.joda.time.DateTimeUtils
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MemoryFeedStoreTest extends FunSuite with FeedStoreTestSupport with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)

  var feedStore: MemoryFeedStore[String] = _

  override protected def beforeEach() = {
    feedStore = createFeedStore
  }

  override protected def afterAll() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  def createUrlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org/feeds")
    override def collectionLink: Url = ???
  }

  def createFeedStore = new MemoryFeedStore[String](
    feedName = "int_feed",
    urlBuilder = createUrlBuilder,
    title = Some("Test"),
  "text/plain"
  )

  test("getFeed returns correct page of the feed") {
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

}
