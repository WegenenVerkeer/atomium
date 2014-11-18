package be.vlaanderen.awv.atom.providers

import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.format._
import be.vlaanderen.awv.atom.jformat.JFeed
import be.vlaanderen.awv.ws.ManagedPlayApp
import com.ning.http.client.Response
import com.sun.jersey.api.json.JSONJAXBContext
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.DateTime
import play.api.http.HeaderNames
import play.api.libs.ws.{WS, WSClient}
import support.JaxbSupport

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

//TODO long polling support: see https://github.com/EventStore/EventStore/wiki/HTTP-LongPoll-Header
class PlayWsBlockingFeedProvider[T <: FeedContent](feedUrl:String,
                                                   var feedPosition: Option[FeedPosition],
                                                   contentType: String = "application/xml",
                                                   timeout:Duration = 30.seconds,
                                                   wsClient:Option[WSClient] = None)
                                                  (implicit jaxbContext:JSONJAXBContext)
  extends FeedProvider[T] with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  type FeedResult =  Try[Feed[T]]
  type FutureResult = Future[FeedResult]

  private var managedPlayAppOpt : Option[ManagedPlayApp] = None

  implicit lazy val playApp = managedPlayAppOpt
                              .getOrElse(sys.error("Play Application not started!"))
                              .playApp

  override def start() : Unit = {
    val managedPlayApp = new ManagedPlayApp
    managedPlayApp.onStart()
    managedPlayAppOpt = Some(managedPlayApp)

  }

  override def stop() : Unit = {
    managedPlayAppOpt.map(_.onStop())
  }

  override def fetchFeed(): FeedResult = {
    initialPosition match {
      case None => awaitResult(fetchFeedAsync)
      case Some(position) => awaitResult(fetchFeedAsync(position.url.path, position.headers))
    }

  }

  override def fetchFeed(page: String): FeedResult = {
    awaitResult(fetchFeedAsync(page))
  }


  /**
   * Fetch the Feed for the given page
   */
  def fetchFeedAsync(page:String, headers: Map[String, String] = Map.empty): FutureResult = fetch(page, headers)

  /**
   * Fetch the first Feed page
   */
  def fetchFeedAsync: FutureResult = {

    def fetchLastFeed(feedResult:FeedResult) : FutureResult = {
      feedResult match {
        // Success? we must have a Feed with a lastLink
        case s @ Success(feed) => feed.lastLink match {
          case Some(link) => fetch(feed.resolveUrl(link.href).path)
          // this can happen in case of 304 not modified, returns an empty feed !!!
          case None => Future.successful(s)
        }
        // fetch failure? wrap it in a new Future
        // NOTE: although the fetch is a Failure, we should return a Success.
        // The call to the service succeeded, it is the content that is wrong

        case failure @ Failure(_) => Future.successful(failure)
      }
    }

    fetch(feedUrl).flatMap(fetchLastFeed)
  }

  private def awaitResult(futureResult: FutureResult) : FeedResult = {
    try {
      Await.result(futureResult, timeout)
    } catch {
      case e:Exception =>
        logger.error(s"Error while fetching feed", e)
        Failure(FeedProcessingException(None, e.getMessage))
    }
  }


  private def fetch(url:String, headers: Map[String, String] = Map.empty) : FutureResult = {
    logger.info(s"fetching $url")
    val wsResponse = wsClient.getOrElse(WS.client).url(url).withHeaders("Accept" -> contentType).
      withHeaders(headers.toSeq: _*).get()
    wsResponse.map { res =>
      logger.info(s"response status: %{res.statusText}")
      res.status match {
        case 200 => {
          val bytes = res.underlying.asInstanceOf[Response].getResponseBodyAsBytes
          val feed: Feed[T] = jFeed2Feed(res.header(HeaderNames.CONTENT_TYPE) match {
            case Some("application/json") => JaxbSupport.fromJsonBytes(bytes, "UTF-8", classOf[JFeed[T]])
            case _ => JaxbSupport.fromXmlBytes(bytes, "UTF-8", classOf[JFeed[T]])
          })
          Success(feed.copy(headers = transformHeaders(res.allHeaders)))
        }
        case 304 => Success(new Feed[T](Url(url), format.randomUuidUri, None, None, new DateTime(),
          List(Link(Link.selfLink, Url(url))), Nil))
        case _   => Failure(new FeedProcessingException(None, s"${res.status}: ${res.statusText}"))
      }
    }
  }

  private def transformHeaders(allHeaders : Map[String, Seq[String]]): Map[String, String] = {
    //null check is needed for MockWS bug, which does not set allHeaders on mocked WSResponse
    val headers = if (allHeaders == null) Map.empty else allHeaders
    headers collect {
      case ("ETag", s) => ("If-None-Match", s(0))
      case ("Last-Modified", s) => ("If-Modified-Since", s(0))
    }
  }

  override def initialPosition: Option[FeedPosition] = feedPosition
}
