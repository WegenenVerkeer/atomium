package be.vlaanderen.awv.atom.providers

import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.ws.ManagedPlayApp
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.LocalDateTime
import play.api.http.HeaderNames
import play.api.libs.ws.{WS, WSClient}

import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}


/**
 * An implementation of [[be.vlaanderen.awv.atom.FeedProvider]] that uses the Play WS API for fetching feed pages via
 * HTTP.
 *
 * Although the Play WS API offers an async interface, this class blocks on request since the [[be.vlaanderen.awv.atom.FeedProvider]]
 * doesn't offer an async interface yet.
 *
 * If you want to use this implementation you will need to add a dependency on the Play WS API library.
 *
 * TODO add long polling support: see https://github.com/EventStore/EventStore/wiki/HTTP-LongPoll-Header
 *
 * @param feedUrl the url of the feed that will be fetched
 * @param timeout the HTTP connection timeout
 *
 * @tparam T the type of the entries in the feed
 */
class PlayWsBlockingFeedProvider[T](feedUrl:String,
                                    feedPosition: Option[FeedPosition],
                                    feedUnmarshaller: FeedUnmarshaller[T],
                                    contentType: String = "application/xml",
                                    timeout:Duration = 30.seconds,
                                    wsClient:Option[WSClient] = None)

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
      case Some(position) => awaitResult(fetchFeedAsync(position.url.path))
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
      case NonFatal(e) =>
        logger.error(s"Error while fetching feed", e)
        Failure(FeedProcessingException(None, e.getMessage))
    }
  }

  private def fetch(url:String, headers: Map[String, String] = Map.empty) : FutureResult = {

    logger.info(s"fetching $url")

    val wsResponse = wsClient
                     .getOrElse(WS.client)
                     .url(url)
                     .withHeaders("Accept" -> contentType)
                     .withHeaders(headers.toSeq: _*).get()

    wsResponse.map { res =>
      logger.info(s"response status: %{res.statusText}")

      res.status match {
        case 200 =>
          feedUnmarshaller
          .unmarshal(res.header(HeaderNames.CONTENT_TYPE), res.body)
          .map(_.copy(headers = transformHeaders(res.allHeaders)))

        case 304 => Success(
          new Feed[T](
            "id",
            Url(url),
            None,
            None,
            new LocalDateTime(),
            List(Link(Link.selfLink, Url(url))),
            Nil
          )
        )

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
