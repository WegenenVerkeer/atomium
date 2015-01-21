package be.wegenenverkeer.atom.providers

import be.wegenenverkeer.atom._
import be.wegenenverkeer.atom.async.AsyncFeedProvider
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.LocalDateTime
import play.api.http.HeaderNames
import play.api.libs.ws.WSClient

import scala.concurrent.Future


/**
 * An implementation of [[be.wegenenverkeer.atom.FeedProvider]] that uses the Play WS API for fetching feed pages via
 * HTTP.
 *
 * If you want to use this implementation you will need to add a dependency on the Play WS API library.
 *
 * TODO add long polling support: see https://github.com/EventStore/EventStore/wiki/HTTP-LongPoll-Header
 *
 * @param feedUrl the pageUrl of the feed that will be fetched
 *
 * @tparam T the type of the entries in the feed
 */
class PlayWsFeedProvider[T](feedUrl:String,
                            val initialEntryRef: Option[EntryRef],
                            feedUnmarshaller: FeedUnmarshaller[T],
                            wsClient:WSClient,
                            contentType: String = "application/xml",
                            headers: Map[String, String] = Map.empty) extends AsyncFeedProvider[T] with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def fetchFeed(): Future[Feed[T]] = {
    initialEntryRef match {
      case None => fetchFeed(feedUrl).flatMap(fetchLastFeed)
      case Some(position) => fetchFeed(position.url.path)
    }
  }

  override def fetchFeed(pageUrl: String): Future[Feed[T]] = {
    logger.info(s"fetching $pageUrl")

    val wsResponse = wsClient
                     .url(pageUrl)
                     .withHeaders("Accept" -> contentType)
                     .withHeaders(headers.toSeq: _*).get()

    wsResponse.map { res =>
      logger.info(s"response status: %{res.statusText}")

      res.status match {
        case 200 =>
          feedUnmarshaller
          .unmarshal(res.header(HeaderNames.CONTENT_TYPE), res.body)
          .map(_.copy(headers = transformHeaders(res.allHeaders)))
          .get


        case 304 =>
            new Feed[T](
              id = "N/A",
              base = Url(pageUrl),
              title = None,
              generator = None,
              updated = new LocalDateTime(),
              links = List(Link(Link.selfLink, Url(pageUrl))),
              entries = Nil
            )

        case _   => throw new FeedProcessingException(None, s"${res.status}: ${res.statusText}")
      }
    }
  }

  /**
   * Fetch the last Feed page
   */
  private def fetchLastFeed(feed:Feed[T]) : Future[Feed[T]] = {
    feed.lastLink match {

      case Some(link) => fetchFeed(feed.resolveUrl(link.href).path)

      // this can happen in case of 304 not modified, returns an empty feed !!!
      case None => Future.successful(feed)

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

}
