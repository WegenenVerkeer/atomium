package controllers

import be.wegenenverkeer.atomium.play.PlayJsonCodec
import be.wegenenverkeer.atomium.server.Context
import be.wegenenverkeer.atomium.server.play.FeedSupport
import play.api.mvc.Controller

import scala.concurrent.Future

/**
 * This controller serves pages of a feed containing Strings.
 * It only supports a json marshaller and thus only supports JSON responses
 */
class StringController() extends Controller with FeedSupport[String] {
  
  lazy val feedService = Deps.stringService

  implicit val context: Context = new Context {} //dummy context for MemoryFeedStore


  override def marshallers = {
    case Accepts.Json() => PlayJsonCodec()
  }



  /**
   * @return the head of the feed
   */
  def headOfFeed() = {
    processFeedPage(Future.successful(Some(feedService.getHeadOfFeed())))
  }

  /**
   *
   * @param start - start of feed (exclusive)
   * @param pageSize - page size
   * @param forward - if true navigates forward else backward
   * @return a page of the feed
   */
  def getFeedPage(start: Int, pageSize: Int, forward: Boolean) = {
    processFeedPage(feedService.getFeedPage(start, pageSize, forward))
  }

}
