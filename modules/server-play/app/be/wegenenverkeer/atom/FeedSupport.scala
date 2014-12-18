package be.wegenenverkeer.atom

import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, Duration}
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc._

/**
 * trait supporting serving of feed pages:
 * sets correct caching headers
 * sets ETag and Last-Modified response headers and responds with Not-Modified if needed to reduce bandwidth
 * supports content-negotiation and responds with either JSON or XML depending on registered marshallers
 *
 * @tparam T the type of the feed entries²
 */
trait FeedSupport[T] extends Results with HeaderNames with Rendering with AcceptExtractors with Logging {

  type FeedMarshaller = Feed[T] => Array[Byte]

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  private val generator = Generator("atomium", Some(Url("http://github.com/WegenenVerkeer/atomium")), Some("0.0.1"))

  val rfcFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss z")
  val rfcUTCFormat = rfcFormat.withZoneUTC()

  private var marshallerRegistry: Map[String, FeedMarshaller] = Map.empty

  /**
   * register a marshaller, i.e. a function which turns a Feed[T] into an Array[Byte] for a specific mime-type
   * @param mimeType the mime-type
   * @param marshaller the marshaller
   */
  def registerMarshaller(mimeType: String, marshaller: FeedMarshaller): Unit = {
    marshallerRegistry += mimeType -> marshaller
  }

  /**
   * marshall the feed and set correct headers
   * @param page the optional page of the feed
   * @param codec the implicit codec
   * @return the response
   */
  def processFeedPage(page: Option[Feed[T]])(implicit codec: Codec) = Action { implicit request =>
    logger.info(s"processing request: $request")
    page match {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          logger.info("sending response: 304 Not-Modified")
          NotModified
        } else {
          //add generator
          val feed: Feed[T] = f.copy(generator = Some(generator))
          render {
            case Accepts.Json() if marshallerRegistry.contains(MimeTypes.JSON) =>
              marshall(marshallerRegistry.get(MimeTypes.JSON), feed).as(MimeTypes.JSON)

            case Accepts.Xml()  if marshallerRegistry.contains(MimeTypes.XML)  =>
              marshall(marshallerRegistry.get(MimeTypes.XML), feed).as(MimeTypes.XML)
          }
        }
      case None =>
        logger.info("sending response: 404 Not-Found")
        NotFound("feed or page not found")
    }
  }

  private[this] def marshall(marshaller: Option[FeedMarshaller], feed: Feed[T]) = {
    marshaller.fold(NotAcceptable: Result){ (m: FeedMarshaller) =>
      //marshall feed and add Last-Modified header
      logger.info("sending response: 200 Found")
      val result = Ok(m(feed))
        .withHeaders(LAST_MODIFIED -> rfcUTCFormat.print(feed.updated.toDateTime), ETAG -> feed.calcETag)

      //add extra cache headers or forbid caching
      if (feed.complete()) {
        val expires = new DateTime().withDurationAdded(new Duration(cacheTime * 1000L), 1)
        result.withHeaders(CACHE_CONTROL -> {
          "public, max-age=" + cacheTime
        }, EXPIRES -> rfcUTCFormat.print(expires))
      } else {
        result.withHeaders(CACHE_CONTROL -> "public, max-age=0, no-cache, must-revalidate")
      }
    }
  }

  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 15:00:00 => modified => false
  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 10:00:00 => not modified => true
  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 12:00:00 => not modified => true
  private def notModified(f: Feed[T], headers: Headers): Boolean = {
    (headers get IF_NONE_MATCH exists { _ == f.calcETag }) ||
    (headers get IF_MODIFIED_SINCE exists { dateStr => try {
        rfcFormat.parseDateTime(dateStr).toDate.getTime >= f.updated.toDate.getTime
      } catch {
        case e: IllegalArgumentException =>
          logger.error(e.getMessage, e)
          false
      }
    })
  }

}
