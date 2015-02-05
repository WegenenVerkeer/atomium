import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.{Context, FeedStoreTestSupport, MemoryFeedStore, UrlBuilder}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class MemoryFeedStoreTest extends FunSuite with FeedStoreTestSupport with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("getFeed returns correct page of the feed") { implicit context =>
    val feedStore = createFeedStore
    testFeedStorePaging(feedStore = feedStore, pageSize = 3)
  }

  def test(description: String)(block: Context => Unit): Unit = {
    block(new Context {})
  }

  def createFeedStore(implicit context: Context) = new MemoryFeedStore[String, Context](
    feedName = "int_feed",
    url = Url("http://www.example.org/feeds"),
    title = Some("Test"),
    "text/plain"
  )


}
