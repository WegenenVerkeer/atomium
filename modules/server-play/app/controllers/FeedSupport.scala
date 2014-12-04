package controllers

import be.wegenenverkeer.atom.{Generator, Url, Feed, Marshallers}
import Marshallers._
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat
import play.api.http.{ContentTypeOf, ContentTypes, HeaderNames, Writeable}
import play.api.mvc._

trait FeedSupport[T] extends Results with HeaderNames with Logging {

  def jsonMarshaller: JsonMarshaller[Feed[T]]
  def xmlMarshaller: XmlMarshaller[Feed[T]]

  implicit def contentTypeOf_Feed(implicit codec: Codec, header: RequestHeader): ContentTypeOf[Feed[T]] = ContentTypeOf[Feed[T]] {
    header.headers.get(HeaderNames.ACCEPT) match {
      case Some("application/json") => Some(ContentTypes.JSON)
      case _                        => Some(ContentTypes.XML)
    }
  }

  implicit def writeableOf_ContentNeg(implicit codec: Codec, header: RequestHeader): Writeable[Feed[T]] = {
    Writeable[Feed[T]] {
      f: Feed[T] => header.headers.get(HeaderNames.ACCEPT) match {
        case Some("application/json") => jsonMarshaller(f)
        case _                        => xmlMarshaller(f)
      }
    }
  }

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  private val generator = Generator("atomium", Some(Url("http://github.com/WegenenVerkeer/atomium")), Some("0.0.1"))

  val rfcFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZoneUTC()

  protected def processFeedPage(page: Option[Feed[T]])(implicit codec: Codec) = Action { implicit request =>
    logger.info(s"processing request: $request")
    page match {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          logger.info("sending response: 304 Not-Modified")
          NotModified
        } else {
          val result: Result = Ok(f.copy(generator = Some(generator))).
            withHeaders(LAST_MODIFIED -> rfcFormat.print(f.updated), ETAG -> f.calcETag)
          logger.info("sending response: 200 Found")
          //add extra cache headers or forbid caching
          if (f.complete()) {
            val expires = new LocalDateTime(System.currentTimeMillis() + (cacheTime * 1000L))
            result.withHeaders(CACHE_CONTROL -> { "public, max-age=" + cacheTime },
              EXPIRES -> expires.toString("EEE, dd MMM yyyy HH:mm:ss z"))
          } else {
            result.withHeaders(CACHE_CONTROL -> "public, max-age=0, no-cache, must-revalidate")
          }
        }
      case None =>
        logger.info("sending response: 404 Not-Found")
        NotFound("feed or page not found")
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
