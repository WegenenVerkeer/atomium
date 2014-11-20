package controllers

import be.vlaanderen.awv.atom.{Feed, Context, FeedService}
import org.joda.time.LocalDateTime
import play.api.libs.json.JsValue
import play.api.mvc.{Action, Controller, Headers, Result}

abstract class FeedController[T](feedService: FeedService[T, Context]) extends Controller {

  type FeedMarshaller = Feed[T] => JsValue

  private val cacheTime = 60 * 60 * 24 * 365 //365 days, 1 year (approximately)

  /**
   * @param c context needed to retrieve the feed
   * @param m marshaller to marshall from a Feed[T] to json
   * @return the head of the page
   */
  def getHeadOfFeed(implicit c: Context, m: FeedMarshaller) = {
    processFeedPage(feedService.getHeadOfFeed())
  }

  /**
   *
   * @param start
   * @param pageSize
   * @param c context needed to retrieve the feed
   * @param m marshaller to marshall from a Feed[T] to json
   * @return a page of the feed
   */
  def getFeedPage(start: Int, pageSize: Int)(implicit c: Context, m: FeedMarshaller) = {
    processFeedPage(feedService.getFeedPage(start, pageSize))
  }

  private def processFeedPage(page: Option[Feed[T]])(implicit m: FeedMarshaller) = Action { implicit request =>
    page match {
      case Some(f) =>
        if (notModified(f, request.headers)) {
          NotModified
        } else {
          val result: Result = Ok(m(f)).
            withHeaders(LAST_MODIFIED -> f.updated, ETAG -> f.calcETag())
          //add extra cache headers or forbid caching
          if (f.complete()) {
            val expires = new LocalDateTime(System.currentTimeMillis() + (cacheTime * 1000L))
            result.withHeaders(CACHE_CONTROL -> { "public, max-age=" + cacheTime },
              EXPIRES -> expires.toString("EEE, dd MMM yyyy HH:mm:ss z"))
          } else {
            result.withHeaders(CACHE_CONTROL -> "public, max-age=0, no-cache, must-revalidate")
          }
        }
      case None => NotFound("feed or page not found")
    }
  }

  private def notModified(f : Feed[T], headers : Headers) : Boolean = {
    (headers get IF_NONE_MATCH filter { _ == f.calcETag() }).exists(_ => true)
    //TODO check Not-Modified-Since header
  }

}
