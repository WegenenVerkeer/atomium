package be.wegenenverkeer.atomium.server.play

import be.wegenenverkeer.atomium.format._
import be.wegenenverkeer.atomium.play.PlayJsonFormats._
import be.wegenenverkeer.atomium.play.PlayJsonSupport
import org.joda.time.{DateTime, DateTimeUtils}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc.Result

import scala.concurrent.Future

class FeedSupportSuite extends FunSuite with Matchers with OptionValues with BeforeAndAfterAll {

  DateTimeUtils.setCurrentMillisFixed(0)

  override protected def afterAll() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  val incompleteFeed: Feed[String] = new Feed("id",
    Url("http://example.com"), None, None, new DateTime(0), List(Link(Link.selfLink, Url("foo"))), List())

  val completeFeed: Feed[String] = incompleteFeed.copy(links = Link(Link.previousLink, Url("prev")) :: incompleteFeed.links)

  test("processing a None should return Not-Found") {
    val result: Future[Result] = new FeedSupport[Nothing]() {} processFeedPage None apply FakeRequest()
    status(result) shouldBe NOT_FOUND
    contentAsString(result) shouldBe "feed or page not found"
  }

  test("processing a feed with no registered marshallers should return Not-Acceptable") {
    val result: Future[Result] = new FeedSupport[String]() {} processFeedPage Some(incompleteFeed) apply FakeRequest()
    status(result) shouldBe NOT_ACCEPTABLE
  }

  test("processing an incomplete feed should return the marshalled feed with correct headers") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val result: Future[Result] = feedSupport processFeedPage Some(incompleteFeed) apply FakeRequest()
    status(result) shouldBe OK
    contentType(result) shouldBe Some("application/json")
    headers(result).get(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=0, no-cache, must-revalidate")
    headers(result).get(HeaderNames.EXPIRES) shouldBe None
    headers(result).get(HeaderNames.ETAG) shouldBe Some(incompleteFeed.calcETag)
    headers(result).get(HeaderNames.LAST_MODIFIED) shouldBe Some("Thu, 01 Jan 1970 00:00:00 UTC")
  }

  test("processing a complete feed should return the marshalled feed with correct headers") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply FakeRequest()
    status(result) shouldBe OK
    contentType(result) shouldBe Some("application/json")
    headers(result).get(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=31536000")
    headers(result).get(HeaderNames.EXPIRES) shouldBe Some("Fri, 01 Jan 1971 00:00:00 UTC")
    headers(result).get(HeaderNames.ETAG) shouldBe Some(completeFeed.calcETag)
    headers(result).get(HeaderNames.LAST_MODIFIED) shouldBe Some("Thu, 01 Jan 1970 00:00:00 UTC")
  }

  test("processing a unchanged feed should return Not-Modified") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val request = FakeRequest().withHeaders(HeaderNames.IF_NONE_MATCH -> completeFeed.calcETag)
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply request
    status(result) shouldBe NOT_MODIFIED
  }

  test("processing a changed feed should not return Not-Modified") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val request = FakeRequest().withHeaders(HeaderNames.IF_NONE_MATCH -> incompleteFeed.calcETag)
    val changedFeed = incompleteFeed.copy(entries = Entry[String]("id",
      new DateTime(),
      new Content[String]("foo", ""), List()) :: incompleteFeed.entries)
    val result: Future[Result] = feedSupport processFeedPage Some(changedFeed) apply request
    status(result) shouldBe OK
  }

  test("processing a non-updated feed should return Not-Modified") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val request = FakeRequest().withHeaders(HeaderNames.IF_MODIFIED_SINCE -> "Thu, 01 Jan 1970 00:00:00 UTC")
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply request
    status(result) shouldBe NOT_MODIFIED
  }

  test("processing an updated feed should not return Not-Modified") {
    val feedSupport = new FeedSupport[String]() {
      registerMarshaller(MimeTypes.JSON, PlayJsonSupport.jsonMarshaller[Feed[String]])
    }
    val request = FakeRequest().withHeaders(HeaderNames.IF_MODIFIED_SINCE -> "Thu, 01 Jan 1970 00:00:00 UTC")
    val updatedFeed = incompleteFeed.copy(updated = new DateTime(1000)) //1 second later
    val result: Future[Result] = feedSupport processFeedPage Some(updatedFeed) apply request
    status(result) shouldBe OK
  }
}
