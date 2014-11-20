package be.vlaanderen.awv.atom.providers

import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.Formats._
import mockws._
import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._

import scala.util.Success

class PlayWsBlockingFeedProviderTest extends FunSuite with Matchers {

  implicit val unmarshaller : FeedEntryUnmarshaller[String] = {
    responseBody => Success(Json.parse(responseBody).as[Feed[String]])
  }

  test("Feed not found") {
    val notFoundRoute = Route {
      case (GET, u) => Action {
        NotFound("feed not found")
      }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed", None,
        wsClient = Some(MockWS(notFoundRoute))),
      consumedEvents = List(),
      finalPosition = None
    ) assertResult { result =>
      result.isFailure shouldBe true
    }
  }

  test("Feed is empty") {
    val emptyRoute = Route {
      case (GET, u) => Action {
        Ok("")
      }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed", None,
        wsClient = Some(MockWS(emptyRoute))),
      consumedEvents = List(),
      finalPosition = None
    ) assertResult { result =>
      result.isFailure shouldBe true
    }
  }

  val page1: String = """{
                        |  "id" : "page1",
                        |  "base" : "http://example.com/",
                        |  "title" : "title",
                        |  "updated" : "update",
                        |  "links" : [ {
                        |    "rel" : "self",
                        |    "href" : "feed/1"
                        |  }, {
                        |    "rel" : "last",
                        |    "href" : "feed/1"
                        |  }, {
                        |    "rel" : "previous",
                        |    "href" : "feed/2"
                        |  } ],
                        |  "entries" : [ {
                        |    "content" : {
                        |      "value" : [ "a1", "b1", "c1" ],
                        |      "rawType" : "application/json"
                        |    },
                        |    "links" : [ ]
                        |  }, {
                        |    "content" : {
                        |      "value" : [ "d1", "e1" ],
                        |      "rawType" : "application/json"
                        |    },
                        |    "links" : [ ]
                        |  }]
                        |}
                        | """.stripMargin

  val page2: String = """{
                        |  "id" : "page2",
                        |  "base" : "http://example.com/",
                        |  "title" : "title",
                        |  "updated" : "update",
                        |  "links" : [ {
                        |    "rel" : "self",
                        |    "href" : "feed/2"
                        |  }, {
                        |    "rel" : "last",
                        |    "href" : "feed/1"
                        |  }, {
                        |    "rel" : "next",
                        |    "href" : "feed/1"
                        |  } ],
                        |  "entries" : [ {
                        |    "content" : {
                        |      "value" : [ "a2", "b2", "c2" ],
                        |      "rawType" : "application/json"
                        |    },
                        |    "links" : [ ]
                        |  }, {
                        |    "content" : {
                        |      "value" : [ "d2", "e2" ],
                        |      "rawType" : "application/json"
                        |    },
                        |    "links" : [ ]
                        |  } ]
                        |}
                        | """.stripMargin

  test("feed is consumed from begin to end") {
    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action  { Ok(page2).withHeaders(ETAG -> "foo") }
      case (GET, u) if u === "http://example.com/feed/1" => Action  { Ok(page1).withHeaders(ETAG -> "bar") }
      case (GET, u) if u === "http://example.com/feed/2" => Action { Ok(page2).withHeaders(ETAG -> "foo") }
      case (GET, u) => Action { NotFound(s"$u not found") }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed", None, wsClient = Some(MockWS(route))),
      consumedEvents = List("a1", "b1", "c1", "d1", "e1", "a2", "b2", "c2", "d2", "e2"),
      //finalPosition is on second element (index=1) of second feed page
      finalPosition = Some(FeedPosition(Url("http://example.com/feed/2"), 1))
    )
  }

  test("feed is consumed from initialPosition to end") {
    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action  { Ok(page2).withHeaders(ETAG -> "foo") }
      case (GET, u) if u === "http://example.com/feed/1" => Action  { Ok(page1).withHeaders(ETAG -> "bar") }
      case (GET, u) if u === "http://example.com/feed/2" => Action { Ok(page2).withHeaders(ETAG -> "foo") }
      case (GET, u) => Action { NotFound(s"$u not found") }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed",
        Some(FeedPosition(Url("http://example.com/feed/1"), 0)), wsClient = Some(MockWS(route))),
      consumedEvents = List("d1", "e1", "a2", "b2", "c2", "d2", "e2"),
      //finalPosition is on second element (index=1) of second feed page
      finalPosition = Some(FeedPosition(Url("http://example.com/feed/2"), 1))
    )
  }

  test("initialPosition is start of feed (all entries consumed) so no new entries need to be processed") {
    val route = Route {
      case (GET, u) if u === "http://example.com/feed/2" => Action { Ok(page2).withHeaders(ETAG -> "foo") }
      case (GET, u) => Action { NotFound(s"$u not found") }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed",
        Some(FeedPosition(Url("http://example.com/feed/2"), 1)), wsClient = Some(MockWS(route))),
      consumedEvents = List(),
      finalPosition = None
    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  test("feed head has not changed") {
    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action  { NotModified }
      case (GET, u) => Action { NotFound(s"$u not found") }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed", None, wsClient = Some(MockWS(route))),
      consumedEvents = List(),
      finalPosition = None
    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  test("feed page has not changed") {
    val route = Route {
      case (GET, u) if u === "http://example.com/feed/1" => Action  { NotModified }
      case (GET, u) => Action { NotFound(s"$u not found") }
    }
    Scenario(
      provider = new PlayWsBlockingFeedProvider[String]("http://example.com/feed",
        Some(FeedPosition(Url("http://example.com/feed/1"), 0)), wsClient = Some(MockWS(route))),
      consumedEvents = List(),
      finalPosition = None //since there never was any entry consumed, this is None
    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  case class Scenario(provider:PlayWsBlockingFeedProvider[String],
                      consumedEvents:List[String],
                      finalPosition:Option[FeedPosition]) {

    val consumer = new StatefulEntryConsumer
    val process = new FeedProcessor[String](provider, consumer)

    val result = process.start()

    consumedEvents shouldBe consumer.consumedEvents.toList
    finalPosition shouldBe consumer.finalPosition

    def assertResult(block: FeedProcessingResult => Unit) = block(result)
  }

}
