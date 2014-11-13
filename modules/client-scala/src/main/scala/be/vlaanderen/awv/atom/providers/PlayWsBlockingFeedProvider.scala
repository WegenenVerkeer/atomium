package be.vlaanderen.awv.atom.providers

import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.ws.ManagedPlayApp
import com.typesafe.scalalogging.slf4j.Logging
import play.api.libs.ws.WS

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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
 * @param feedUrl the url of the feed that will be fetched
 * @param timeout the HTTP connection timeout
 *
 * @tparam T the type of the entries in the feed
 */
class PlayWsBlockingFeedProvider[T:FeedEntryUnmarshaller](feedUrl:String,
                                                          feedPosition: Option[FeedPosition],
                                                          timeout:Duration)
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
      case Some(position) => awaitResult(fetchFeedAsync(position.link.href.path))
    }

  }

  override def fetchFeed(page: String): FeedResult = {
    awaitResult(fetchFeedAsync(page))
  }


  /**
   * Fetch the Feed for the given page
   */
  def fetchFeedAsync(page:String) : FutureResult = fetch(page)

  /**
   * Fetch the first Feed page
   */
  def fetchFeedAsync : FutureResult = {

    def fetchLastFeed(feedResult:FeedResult) : FutureResult = {
      feedResult match {
        // Success? we must have a Feed with a lastLink
        case Success(feed) => feed.lastLink match {
          case Some(link) => fetch(link.href.path)
          // !!! can't proceed without a last link - game over !!!
          case None => sys.error("last feed url is empty!!!")
        }
        // Validation failure? wrap it in a new Future
        // NOTE: although the Validation is a Failure, we should return a Success.
        // The call to the service succeeded, is the content that is wrong
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


  private def fetch(url:String) : FutureResult = {
    val wsResponse = WS.url(feedUrl).get()
    wsResponse.map { res =>
      val unmarshaller = implicitly[FeedEntryUnmarshaller[T]]
      unmarshaller(res.body)
    }
  }

  override def initialPosition: Option[FeedPosition] = feedPosition
}
