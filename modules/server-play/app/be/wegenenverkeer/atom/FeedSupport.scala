package be.wegenenverkeer.atom

import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.http.{HeaderNames, MimeTypes}
import play.api.mvc._

/**
 * trait supporting serving of feed pages:
 * sets correct caching headers
 * sets ETag and Last-Modified response headers and responds with Not-Modified if needed to reduce bandwidth
 * supports content-negotiation and responds with either JSON or XML depending on registered marshallers
 *
 * @tparam T
 */
trait FeedSupport[T] extends Results with HeaderNames with Rendering with AcceptExtractors with Logging {

  type FeedMarshaller = Feed[T] => Array[Byte]

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  private val generator = Generator("atomium", Some(Url("http://github.com/WegenenVerkeer/atomium")), Some("0.0.1"))

  val rfcFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC()

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
  protected def processFeedPage(page: Option[Feed[T]])(implicit codec: Codec) = Action { implicit request =>
    logger.info(s"processing request: $request")
    page match {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          logger.info("sending response: 304 Not-Modified")
          NotModified
        } else {
          val feed: Feed[T] = f.copy(generator = Some(generator))
          render {
            case Accepts.Json() if marshallerRegistry.contains(MimeTypes.JSON) =>
              marshall(marshallerRegistry.get(MimeTypes.JSON).get, feed)
            case Accepts.Xml()  if marshallerRegistry.contains(MimeTypes.XML)  =>
              marshall(marshallerRegistry.get(MimeTypes.XML).get, feed)
            case _ => NotAcceptable
          }
        }
      case None =>
        logger.info("sending response: 404 Not-Found")
        NotFound("feed or page not found")
    }
  }

  private[this] def marshall(marshaller: Feed[T] => Array[Byte], feed: Feed[T]): Result = {
    logger.info("sending response: 200 Found")

    val result = Ok(marshaller(feed))
      .withHeaders(LAST_MODIFIED -> rfcFormat.print(feed.updated), ETAG -> feed.calcETag)

    //add extra cache headers or forbid caching
    if (feed.complete()) {
      val expires = new LocalDateTime(System.currentTimeMillis() + (cacheTime * 1000L))
      result.withHeaders(CACHE_CONTROL -> { "public, max-age=" + cacheTime },
        EXPIRES -> expires.toString("EEE, dd MMM yyyy HH:mm:ss z"))
    } else {
      result.withHeaders(CACHE_CONTROL -> "public, max-age=0, no-cache, must-revalidate")
    }

  }

  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 15:00:00 => modified
  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 10:00:00 => not modified
  //if modified since 02-11-2014 12:00:00 and updated on 02-11-2014 12:00:00 => not modified
  private def notModified(f: Feed[T], headers: Headers): Boolean = {
    (headers get IF_NONE_MATCH exists { _ == f.calcETag }) ||
    (headers get IF_MODIFIED_SINCE exists { dateStr => try {
        f.updated.toDateTime.isBefore(rfcFormat.parseDateTime(dateStr))
      } catch {
        case e: IllegalArgumentException =>
          logger.error(e.getMessage, e)
          false
      }
    })
  }

}
