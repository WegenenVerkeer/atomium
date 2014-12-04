package be.wegenenverkeer.atom.providers

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atom.java.{Feed => JFeed}
import be.wegenenverkeer.atom._
import Marshallers._
import be.wegenenverkeer.atom.java.Adapters
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import mockws._
import org.joda.time.LocalDateTime
import org.scalatest.{FunSuite, Matchers}
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._
import support.{JacksonSupport, JaxbSupport}

class PlayWsBlockingFeedProviderTest extends FunSuite with Matchers with FeedUnmarshaller[String] {

  implicit val jaxbContext = JAXBContext.newInstance("be.wegenenverkeer.atom.java")
  val xmlUnmarshaller: XmlUnmarshaller[Feed[String]] = JaxbSupport.jaxbUnmarshaller
                                                       .andThen(JFeedConverters.jFeed2Feed)

  private val objectMapper = new ObjectMapper()

  objectMapper.registerModule(new JodaModule)

  objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)

  implicit val objectReader = objectMapper.reader(new TypeReference[JFeed[String]]() {})

  val jsonUnmarshaller: JsonUnmarshaller[Feed[String]] = JacksonSupport.jacksonUnmarshaller
                                                         .andThen(JFeedConverters.jFeed2Feed)

  test("Feed not found") {

    val notFoundRoute = Route {
      case (GET, u) => Action {
        NotFound("feed not found")
      }
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = None,
        feedUnmarshaller = this,
        wsClient = Some(MockWS(notFoundRoute))
      ),

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

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = None,
        feedUnmarshaller = this,
        wsClient = Some(MockWS(emptyRoute))
      ),

      consumedEvents = List(),

      finalPosition = None

    ) assertResult { result =>
      result.isFailure shouldBe true
    }
  }

  val updated = Adapters.outputFormatterWithSecondsAndOptionalTZ.print(new LocalDateTime())

  val page1: String = """{
                        |  "base" : "http://example.com",
                        |  "id" : "http://example.com/page1",
                        |  "title" : "title1",
                        |  "updated" : "2014-01-01",
                        |  "links" : [
                        |     { "rel" : "self", "href" : "http://example.com/feed/1" },
                        |     { "rel" : "last", "href" : "/feed/1" },
                        |     { "rel" : "previous", "href" : "/feed/2"}
                        |  ],
                        |  "entries" : [
                        |     { "content" : { "value" : "a1", "type" : "text/plain" }},
                        |     { "content" : { "value" : "b1", "type" : "text/plain" }}
                        |  ]
                        |}
                        | """.stripMargin

  val page2: String = """{
                        |  "base" : "http://example.com",
                        |  "id" : "http://example.com/page2",
                        |  "title" : "title2",
                        |  "updated" : "2014-01-01",
                        |  "links" : [
                        |     { "rel" : "self", "href" : "http://example.com/feed/2" },
                        |     { "rel" : "last", "href" : "/feed/1" },
                        |     { "rel" : "next", "href" : "/feed/1" }
                        |  ],
                        |  "entries" : [
                        |     { "content" : { "value" : "a2", "type" : "text/plain" }},
                        |     { "content" : { "value" : "b2", "type" : "text/plain" }}
                        |  ]
                        |}
                        | """.stripMargin

  test("feed is consumed from begin to end") {

    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action {
        Ok(page2).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) if u === "http://example.com/feed/1" => Action {
        Ok(page1).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) if u === "http://example.com/feed/2" => Action {
        Ok(page2).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) => Action {NotFound(s"$u not found")}
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = None,
        feedUnmarshaller = this,
        wsClient = Some(MockWS(route))
      ),

      consumedEvents = List("a1", "b1", "a2", "b2"),

      //finalPosition is on second element (index=1) of second feed page
      finalPosition = Some(FeedPosition(Url("http://example.com/feed/2"), 1))
    )
  }

  test("feed is consumed from initialPosition to end") {

    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action {
        Ok(page2).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) if u === "http://example.com/feed/1" => Action {
        Ok(page1).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) if u === "http://example.com/feed/2" => Action {
        Ok(page2).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) => Action {NotFound(s"$u not found")}
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = Some(FeedPosition(Url("http://example.com/feed/1"), 0)),
        feedUnmarshaller = this,
        wsClient = Some(MockWS(route))
      ),

      consumedEvents = List("b1", "a2", "b2"),

      //finalPosition is on second element (index=1) of second feed page
      finalPosition = Some(FeedPosition(Url("http://example.com/feed/2"), 1))
    )
  }

  test("initialPosition is start of feed (all entries consumed) so no new entries need to be processed") {

    val route = Route {
      case (GET, u) if u === "http://example.com/feed/2" => Action {
        Ok(page2).withHeaders(CONTENT_TYPE -> "application/json")
      }
      case (GET, u) => Action {NotFound(s"$u not found")}
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = Some(FeedPosition(Url("http://example.com/feed/2"), 1)),
        feedUnmarshaller = this,
        wsClient = Some(MockWS(route))
      ),

      consumedEvents = List(),

      finalPosition = None

    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  test("feed head has not changed") {

    val route = Route {
      case (GET, u) if u === "http://example.com/feed" => Action {NotModified}
      case (GET, u) => Action {NotFound(s"$u not found")}
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = None,
        feedUnmarshaller = this,
        wsClient = Some(MockWS(route))
      ),

      consumedEvents = List(),

      finalPosition = None

    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  test("feed page has not changed") {

    val route = Route {
      case (GET, u) if u === "http://example.com/feed/1" => Action {NotModified}
      case (GET, u) => Action {NotFound(s"$u not found")}
    }

    Scenario(

      provider = new PlayWsBlockingFeedProvider[String](
        feedUrl = "http://example.com/feed",
        feedPosition = Some(FeedPosition(Url("http://example.com/feed/1"), 0)),
        feedUnmarshaller = this,
        wsClient = Some(MockWS(route))
      ),

      consumedEvents = List(),

      finalPosition = None //since there never was any entry consumed, this is None

    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  case class Scenario(provider: PlayWsBlockingFeedProvider[String],
                      consumedEvents: List[String],
                      finalPosition: Option[FeedPosition]) {

    val consumer = new StatefulEntryConsumer
    val process  = new FeedProcessor[String](provider, consumer)

    val result = process.start()

    consumedEvents shouldBe consumer.consumedEvents.toList
    finalPosition shouldBe consumer.finalPosition

    def assertResult(block: FeedProcessingResult => Unit) = block(result)
  }

}
