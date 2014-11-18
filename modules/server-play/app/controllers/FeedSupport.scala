package controllers

import be.vlaanderen.awv.atom.format._
import com.sun.jersey.api.json.JSONJAXBContext
import com.typesafe.scalalogging.slf4j.Logging
import org.joda.time.LocalDateTime
import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc._
import support.{Jaxb, JaxbSupport}

trait FeedSupport[T <: FeedContent] extends JaxbSupport with Results with HeaderNames with Logging {

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  private val generator = Generator("atomium", Some(Url("http://github.com/WegenenVerkeer/atomium")), Some("0.0.1"))

  def acceptHeader(headers: Headers): String = headers.get(HeaderNames.ACCEPT).getOrElse(ContentTypes.XML)

  protected def processFeedPage(page: Option[Feed[T]])(implicit codec: Codec, context: JSONJAXBContext) = Action { implicit request =>
    logger.info(s"processing request: $request")
    page match {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          logger.info("sending response: 304 Not-Modified")
          NotModified
        } else {
          val result: Result = Ok(Jaxb(feed2JFeed(f.copy(generator = Some(generator))))).
            withHeaders(LAST_MODIFIED -> outputFormatterWithSecondsAndOptionalTZ.print(f.updated),
              ETAG -> f.calcETag(acceptHeader(request.headers)))
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
      case None => {
        logger.info("sending response: 404 Not-Found")
        NotFound("feed or page not found")
      }
    }
  }

  private def notModified(f : Feed[T], headers : Headers) : Boolean = {
    (headers get IF_NONE_MATCH filter { _ == f.calcETag(acceptHeader(headers)) }).exists(_ => true)
    //TODO check Not-Modified-Since header
  }

}
