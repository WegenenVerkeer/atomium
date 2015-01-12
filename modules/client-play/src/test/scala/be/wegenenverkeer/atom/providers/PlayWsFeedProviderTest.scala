package be.wegenenverkeer.atom.providers

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atom._
import be.wegenenverkeer.atom.java.{Adapters, Feed => JFeed}
import be.wegenenverkeer.ws.ManagedPlayApp
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import mockws._
import org.joda.time.LocalDateTime
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import play.api.http.MimeTypes
import play.api.mvc.Action
import play.api.mvc.Results._
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.util.{Success, Try}

class PlayWsFeedProviderTest extends FunSuite with Matchers with FeedUnmarshaller[String] with BeforeAndAfterAll {

  implicit val jaxbContext = JAXBContext.newInstance("be.wegenenverkeer.atom.java")

  registerUnmarshaller(MimeTypes.XML,
    JaxbSupport.jaxbUnmarshaller.andThen(JFeedConverters.jFeed2Feed))

  private val objectMapper = new ObjectMapper()

  objectMapper.registerModule(new JodaModule)

  objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false)

  implicit val objectReader = objectMapper.reader(new TypeReference[JFeed[String]]() {})




  registerUnmarshaller(MimeTypes.JSON,
    JacksonSupport.jacksonUnmarshaller.andThen(JFeedConverters.jFeed2Feed))

  test("Feed not found") {

    val notFoundRoute = Route {
      case (GET, u) => Action {
        NotFound("feed not found")
      }
    }

    Scenario(

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = None,
        feedUnmarshaller = this,
        wsClient = MockWS(notFoundRoute)
      ),

      consumedEvents = List(),

      lastConsumedEntryId = None

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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = None,
        feedUnmarshaller = this,
        wsClient = MockWS(emptyRoute)
      ),

      consumedEvents = List(),

      lastConsumedEntryId = None

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
                        |     { "id" : "a1", "content" : { "value" : "a1", "type" : "text/plain" }},
                        |     { "id" : "b1", "content" : { "value" : "b1", "type" : "text/plain" }}
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
                        |     { "id" : "a2", "content" : { "value" : "a2", "type" : "text/plain" }},
                        |     { "id" : "b2", "content" : { "value" : "b2", "type" : "text/plain" }}
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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = None,
        feedUnmarshaller = this,
        wsClient = MockWS(route)
      ),

      consumedEvents = List("a1", "b1", "a2", "b2"),

      //last successful entry is second element 'b2' of second feed page
      lastConsumedEntryId = Some(EntryRef(Url("http://example.com/feed/2"), "b2"))
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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = Some(EntryRef(Url("http://example.com/feed/1"), "b1")),
        feedUnmarshaller = this,
        wsClient = MockWS(route)
      ),

      consumedEvents = List("b1", "a2", "b2"),

      //last successful entry is second element 'b2' of second feed page
      lastConsumedEntryId = Some(EntryRef(Url("http://example.com/feed/2"), "b2"))
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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = Some(EntryRef(Url("http://example.com/feed/2"), "b2")),
        feedUnmarshaller = this,
        wsClient = MockWS(route)
      ),

      consumedEvents = List(),

      lastConsumedEntryId = None

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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = None,
        feedUnmarshaller = this,
        wsClient = MockWS(route)
      ),

      consumedEvents = List(),

      lastConsumedEntryId = None

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

      provider = new PlayWsFeedProvider[String](
        feedUrl = "http://example.com/feed",
        initialEntryRef = Some(EntryRef(Url("http://example.com/feed/1"), "a2")),
        feedUnmarshaller = this,
        wsClient = MockWS(route)
      ),

      consumedEvents = List(),

      lastConsumedEntryId = None //since there never was any entry consumed, this is None

    ) assertResult { result =>
      result.isFailure shouldBe false
    }
  }

  case class Scenario(provider: PlayWsFeedProvider[String],
                      consumedEvents: List[String],
                      lastConsumedEntryId: Option[EntryRef]) {

    val syncProvider = new FeedProvider[String] {

      import scala.concurrent.duration._

      override def initialEntryRef: Option[EntryRef] = provider.initialEntryRef

      override def fetchFeed(): Try[Feed[String]] = Try(Await.result(provider.fetchFeed(), 2 seconds))

      override def fetchFeed(pageUrl: String): Try[Feed[String]] = Try(Await.result(provider.fetchFeed(), 2 seconds))
    }

    val consumer = new StatefulEntryConsumer
    val process  = new FeedProcessor[String](syncProvider, consumer)

    val result = Try(process.start())

    consumedEvents shouldBe consumer.consumedEvents.toList
    lastConsumedEntryId shouldBe consumer.consumedEvents.lastOption

    def assertResult(block: Try[AtomResult[String]] => Unit) = block(result)
  }


  class StatefulEntryConsumer extends EntryConsumer[String] {
    import scala.collection.mutable

    var lastConsumedEntryId: Option[String] = None
    var consumedEvents = new mutable.ListBuffer[String]

    override def apply(eventEntry: Entry[String]): FeedProcessingResult[String] = {
      lastConsumedEntryId = Option(eventEntry.id)
      consumedEvents += eventEntry.content.value
      Success(eventEntry)
    }
  }

}