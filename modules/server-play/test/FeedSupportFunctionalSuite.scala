import be.wegenenverkeer.atom._
import be.wegenenverkeer.atomium.format.{Feed, Url}
import be.wegenenverkeer.atomium.play.PlayJsonFormats._
import be.wegenenverkeer.atomium.play.PlayJsonSupport
import be.wegenenverkeer.atomium.server.{Context, FeedService, MemoryFeedStore}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import org.scalatestplus.play.{OneServerPerSuite, WsScalaTestClient}
import play.api.http.{HeaderNames, MimeTypes, Status}
import play.api.mvc.Controller
import play.api.test.{DefaultAwaitTimeout, FakeApplication, FutureAwaits}

class FeedSupportFunctionalSuite
  extends FunSuite with OneServerPerSuite with Matchers with FutureAwaits
  with DefaultAwaitTimeout with Status with WsScalaTestClient with BeforeAndAfterAll {

  val feedName = "functest"
  val feedPath = s"/feeds/$feedName"
  val lastPage = s"/feeds/$feedName/0/forward/2"

  implicit val context: Context = new Context() {}

  //memory feed store does not require a context
  val feedStore = new MemoryFeedStore[String, Context](
    feedName,
    Url(s"http://localhost/feeds/$feedName"),
    None
  )

  val feedService = new FeedService[String, Context](
    feedName = feedName,
    entriesPerPage = 2,
    feedStore = feedStore
  )

  val feedController = new Controller with FeedSupport[String] {

    registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])

    def headOfFeed() = {
      processFeedPage(feedService.getHeadOfFeed())
    }

    def getFeedPage(start: Int, pageSize: Int, forward: Boolean) = {
      processFeedPage(feedService.getFeedPage(start, pageSize, forward))
    }

  }

  implicit override lazy val app: FakeApplication =
    FakeApplication(
      withRoutes = {
        case ("GET", `feedPath`) => feedController.headOfFeed()
        case ("GET", `lastPage`) => feedController.getFeedPage(0, 2, forward = true)
      }
    )

  override protected def beforeAll(): Unit = {
    feedStore.clear()
  }

  test("get head of empty feed should return Not-Found") {
    val response = await(wsUrl(feedPath).get())
    response.status shouldBe NOT_FOUND
  }

  test("get head of non-empty feed should return marshalled feed with correct headers") {
    feedService.push("foo")

    val response = await(wsUrl(feedPath).get())
    response.status shouldBe OK
    response.header(HeaderNames.CONTENT_TYPE) shouldBe Some("application/json")
    response.header(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=0, no-cache, must-revalidate")
    response.header(HeaderNames.EXPIRES) should be(None)
    response.header(HeaderNames.ETAG) shouldNot be(None)

  }

  test("get completed feed page should return marshalled feed with correct headers") {
    feedService.push("foo")
    feedService.push("bar")
    feedService.push("baz")

    val response = await(wsUrl(lastPage).get())
    response.status shouldBe OK
    response.header(HeaderNames.CONTENT_TYPE) shouldBe Some("application/json")
    response.header(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=31536000")
    response.header(HeaderNames.EXPIRES) shouldNot be(None)
    response.header(HeaderNames.ETAG) shouldNot be(None)
  }

  test("processing a unchanged feed should return Not-Modified") {
    feedService.push("foo")

    val response = await(wsUrl(feedPath).get())
    response.status shouldBe OK
    val etag = response.header(HeaderNames.ETAG)

    val notModified = await(wsUrl(feedPath).withHeaders(HeaderNames.IF_NONE_MATCH -> etag.get) get())
    notModified.status shouldBe NOT_MODIFIED
  }

  test("processing a changed feed should not return Not-Modified") {
    feedService.push("foo")

    val response = await(wsUrl(feedPath).get())
    response.status shouldBe OK
    val etag = response.header(HeaderNames.ETAG)

    //modify feed
    feedService.push("bar")
    val modified = await(wsUrl(feedPath).withHeaders(HeaderNames.IF_NONE_MATCH -> etag.get) get())
    modified.status shouldBe OK
  }

  test("processing a non-updated feed should return Not-Modified") {
    feedService.push("foo")

    val response = await(wsUrl(feedPath).get())
    response.status shouldBe OK
    val lastModified = response.header(HeaderNames.LAST_MODIFIED)

    val notModified = await(wsUrl(feedPath).withHeaders(HeaderNames.IF_MODIFIED_SINCE -> lastModified.get) get())
    notModified.status shouldBe NOT_MODIFIED
  }

  test("processing an updated feed should not return Not-Modified") {

    feedStore.clear()
    feedStore.count shouldBe 0
    feedService.push("foo")
    feedStore.count shouldBe 1

    val response = await(wsUrl(feedPath).get())
    response.status shouldBe OK
    val lastModified = response.header(HeaderNames.LAST_MODIFIED)

    //updates to a feed happening during the same second are not detected by Last-Modified/If-Modified-Since check
    //this is not a problem because such changes will be caught by ETag/If-None-Match check

    //so sleep for a second in this test
    Thread.sleep(1000)
    feedService.push("bar")
    feedStore.count shouldBe 2

    val modified = await(wsUrl(feedPath).withHeaders(HeaderNames.IF_MODIFIED_SINCE -> lastModified.get) get())
    modified.status shouldBe OK
  }


}
