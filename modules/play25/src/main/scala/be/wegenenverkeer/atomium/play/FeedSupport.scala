package be.wegenenverkeer.atomium.play

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

import be.wegenenverkeer.atomium.api.{FeedPage, FeedPageCodec}
import be.wegenenverkeer.atomium.format.Generator
import org.slf4j.LoggerFactory
import play.api.http.{HeaderNames, MediaRange}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

/**
 * trait supporting serving of feed pages:
 * sets correct caching headers
 * sets ETag and Last-Modified response headers and responds with Not-Modified if needed to reduce bandwidth
 * supports content-negotiation and responds with either JSON or XML depending on registered marshallers
 *
 * @tparam T the type of the feed entries²
 */
trait FeedSupport[T] extends Results with HeaderNames with Rendering with AcceptExtractors {

  private val logger = LoggerFactory.getLogger(getClass)

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  private val generator = new Generator("atomium", "http://github.com/WegenenVerkeer/atomium", "0.0.1")

  /**
   * Define the PartialFunction that will map acceptable content types to FeedMarshallers.
   *
   *
   * For instance:
   * {{{
   *   // supports XML and JSON and used predefined FeedMarshallers
   *   override def marshallers = {
   *     case Accepts.Xml()  => JaxbFeedMarshaller[String]()
   *     case Accepts.Json() => PlayJsonFeedMarshaller[String]()
   *   }
   * }}}
   *
   * or in case a specifc content types is need...
   *
   * {{{
   *   // supports XML and JSON and used predefined FeedMarshallers
   *   override def marshallers = {
   *     case Accepts.Xml()  => JaxbFeedMarshaller[String]("application/my-api-v1.0+xml")
   *     case Accepts.Json() => PlayJsonFeedMarshaller[String]("application/my-api-v1.0+json")
   *   }
   * }}}
   *
   *
   * @return `PartialFunction[MediaRange, FeedMarshaller]`
   */
  def marshallers: PartialFunction[MediaRange, FeedPageCodec[T, Array[Byte]]]

  private def buildRenders(feed: FeedPage[T]): PartialFunction[MediaRange, Result] =
    new PartialFunction[MediaRange, Result] {
      override def isDefinedAt(x: MediaRange): Boolean = marshallers.isDefinedAt(x)

      override def apply(v1: MediaRange): Result = {
        val feedMarshaller = marshallers.apply(v1)
        marshall(feedMarshaller, feed)
      }
    }


  /**
   * marshall the feed and set correct headers
   * @param page the optional page of the feed
   * @param codec the implicit codec
   * @return the response
   */
  def processFeedPage(page: Future[Option[FeedPage[T]]])(implicit codec: Codec) = Action.async { implicit request =>
    logger.info(s"processing request: $request")
    page.map {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          logger.info("sending response: 304 Not-Modified")
          NotModified
        } else {
          //add generator
          f.setGenerator(generator)
          render(buildRenders(f))
        }
      case None    =>
        logger.info("sending response: 404 Not-Found")
        NotFound("feed or page not found")
    }
  }

  def processFeedPage(page: Option[FeedPage[T]])(implicit codec: Codec): Action[AnyContent] = {
    processFeedPage(Future.successful(page))
  }

  private def marshall(codec: FeedPageCodec[T, Array[Byte]], feed: FeedPage[T]): Result = {
    //marshall feed and add Last-Modified header

    val (contentType, payload) = (codec.getMimeType, codec.encode(feed))

    logger.info("sending response: 200 Found")
    val result = Ok(payload)
      .withHeaders(LAST_MODIFIED -> feed.getUpdated().format(DateTimeFormatter.RFC_1123_DATE_TIME), ETAG -> feed.calcETag)

    //add extra cache headers or forbid caching
    val resultWithCacheHeader =
      if (feed.complete()) {
        val expires = OffsetDateTime.now().plusSeconds(1000L)
        result.withHeaders(CACHE_CONTROL -> {
          "public, max-age=" + cacheTime
        }, EXPIRES -> DateTimeFormatter.RFC_1123_DATE_TIME.format(expires))
      } else {
        result.withHeaders(CACHE_CONTROL -> "public, max-age=0, no-cache, must-revalidate")
      }

    resultWithCacheHeader.as(contentType)
  }

  //if modified since 02-11-2014 12:00:00 and getUpdated on 02-11-2014 15:00:00 => modified => false
  //if modified since 02-11-2014 12:00:00 and getUpdated on 02-11-2014 10:00:00 => not modified => true
  //if modified since 02-11-2014 12:00:00 and getUpdated on 02-11-2014 12:00:00 => not modified => true
  private def notModified(f: FeedPage[T], headers: Headers): Boolean = {

    val ifNoneMatch = headers get IF_NONE_MATCH exists ( _ ==  f.calcETag )

    val ifModifiedSince = headers get IF_MODIFIED_SINCE exists { dateStr =>
      try {
        val updated = f.getUpdated.`with`(ChronoField.MILLI_OF_SECOND, 0)
        OffsetDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME).compareTo(updated) >= 0
      }
      catch {
        case e: IllegalArgumentException =>
          logger.error(e.getMessage, e)
          false
      }
    }

    ifNoneMatch || ifModifiedSince
  }

}
