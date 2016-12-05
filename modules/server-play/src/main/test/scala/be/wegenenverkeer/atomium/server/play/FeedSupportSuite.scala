package be.wegenenverkeer.atomium.server.play

import java.time.OffsetDateTime

import be.wegenenverkeer.atomium.api.{Entry, FeedPage}
import be.wegenenverkeer.atomium.format._
import be.wegenenverkeer.atomium.play.PlayJsonCodec
import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers, OptionValues}
import play.api.http.HeaderNames
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class FeedSupportSuite extends FunSuite with Matchers with OptionValues with BeforeAndAfterAll {

  DateTimeUtils.setCurrentMillisFixed(0)

  override protected def afterAll() = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  import scala.collection.JavaConverters._

  val incompleteFeed: FeedPage[String] = new FeedPage[String]("id",
    "http://example.com", "No Title", new Generator("Test generator", "http://test", "1.0"), OffsetDateTime.now(), List(new Link(Link.SELF, "/foo")).asJava, List().asJava)

  val completeFeed: FeedPage[String] = new FeedPage[String]("id",
    "http://example.com", "No Title", new Generator("Test generator", "http://test", "1.0"), OffsetDateTime.now(), List(new Link(Link.SELF, "/foo"), new Link(Link.PREVIOUS, "/prev")).asJava, List().asJava)

  test("processing a None should return Not-Found") {
    val result: Future[Result] = new FeedSupport[Nothing]() {
      override def marshallers = PartialFunction.empty
    } processFeedPage None apply FakeRequest()
    status(result) shouldBe NOT_FOUND
    contentAsString(result) shouldBe "feed or page not found"
  }

  test("processing a feed with no registered marshallers should return Not-Acceptable") {
    val result: Future[Result] = new FeedSupport[String]() {
      override def marshallers = PartialFunction.empty
    } processFeedPage Some(incompleteFeed) apply FakeRequest()
    status(result) shouldBe NOT_ACCEPTABLE
  }

  test("processing an incomplete feed should return the marshalled feed with correct headers") {
    val feedSupport = new JsonFeedSupport
    val result: Future[Result] = feedSupport processFeedPage Some(incompleteFeed) apply FakeRequest()
    status(result) shouldBe OK
    contentType(result) shouldBe Some("application/json")
    headers(result).get(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=0, no-cache, must-revalidate")
    headers(result).get(HeaderNames.EXPIRES) shouldBe None
    headers(result).get(HeaderNames.ETAG) shouldBe Some(incompleteFeed.calcETag)
    headers(result).get(HeaderNames.LAST_MODIFIED) shouldBe Some("Thu, 01 Jan 1970 00:00:00 UTC")
  }

  test("processing a complete feed should return the marshalled feed with correct headers") {
    val feedSupport = new JsonFeedSupport
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply FakeRequest()
    status(result) shouldBe OK
    contentType(result) shouldBe Some("application/json")
    headers(result).get(HeaderNames.CACHE_CONTROL) shouldBe Some("public, max-age=31536000")
    headers(result).get(HeaderNames.EXPIRES) shouldBe Some("Fri, 01 Jan 1971 00:00:00 UTC")
    headers(result).get(HeaderNames.ETAG) shouldBe Some(completeFeed.calcETag)
    headers(result).get(HeaderNames.LAST_MODIFIED) shouldBe Some("Thu, 01 Jan 1970 00:00:00 UTC")
  }

  test("processing a unchanged feed should return Not-Modified") {
    val feedSupport = new JsonFeedSupport
    val request = FakeRequest().withHeaders(HeaderNames.IF_NONE_MATCH -> completeFeed.calcETag)
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply request
    status(result) shouldBe NOT_MODIFIED
  }

//  test("processing a changed feed should not return Not-Modified") {
//    val feedSupport = new JsonFeedSupport
//    val request = FakeRequest().withHeaders(HeaderNames.IF_NONE_MATCH -> incompleteFeed.calcETag)
//    val changedFeed = incompleteFeed.copy(entries = AtomEntry[String]("id",
//      new DateTime(),
//      new Content[String]("foo", ""), List()) :: incompleteFeed.getEntries)
//    val result: Future[Result] = feedSupport processFeedPage Some(changedFeed) apply request
//    status(result) shouldBe OK
//  }

  test("processing a non-updated feed should return Not-Modified") {
    val feedSupport = new JsonFeedSupport
    val request = FakeRequest().withHeaders(HeaderNames.IF_MODIFIED_SINCE -> "Thu, 01 Jan 1970 00:00:00 UTC")
    val result: Future[Result] = feedSupport processFeedPage Some(completeFeed) apply request
    status(result) shouldBe NOT_MODIFIED
  }

//  test("processing an updated feed should not return Not-Modified") {
//    val feedSupport = new JsonFeedSupport
//    val request = FakeRequest().withHeaders(HeaderNames.IF_MODIFIED_SINCE -> "Thu, 01 Jan 1970 00:00:00 UTC")
//    val updatedFeed = incompleteFeed.copy(updated = new DateTime(1000)) //1 second later
//    val result: Future[Result] = feedSupport processFeedPage Some(updatedFeed) apply request
//    status(result) shouldBe OK
//  }

  class JsonFeedSupport extends FeedSupport[String] {
    override def marshallers = {
      case Accepts.Json() => new PlayJsonCodec
    }
  }
}
