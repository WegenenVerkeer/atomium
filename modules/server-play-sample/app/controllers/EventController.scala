package controllers

import be.wegenenverkeer.atomium.api.FeedPage
import be.wegenenverkeer.atomium.format.JaxbCodec
import be.wegenenverkeer.atomium.play.{PlayJaxbCodec, PlayJsonCodec}
import be.wegenenverkeer.atomium.server.Context
import be.wegenenverkeer.atomium.server.play.FeedSupport
import play.api.mvc.Controller

/**
 * This controller serves pages of a feed containing {{Event}}.
 * It registers both a Play json marshaller and a Jaxb xml marshaller
 * and thus supports content negotiation and can support both xml or json responses
 */
class EventController() extends Controller with FeedSupport[Event] {

  lazy val feedService = Deps.eventService

  implicit val context: Context = new Context {} //dummy context for MemoryFeedStore

  //play json marshaller

  import controllers.EventFormat._


  override def marshallers = {
    case Accepts.Xml()  => PlayJaxbCodec(classOf[Event])
    case Accepts.Json() => PlayJsonCodec()
  }

  /**
   * @return the head of the page
   */
  def headOfFeed() = {
    processFeedPage(Some(feedService.getHeadOfFeed()))
  }

  /**
   *
   * @param start - start of feed
   * @param pageSize - page size
   * @return a page of the feed
   */
  def getFeedPage(start: Int, pageSize: Int, forward: Boolean) = {
    processFeedPage(feedService.getFeedPage(start, pageSize, forward))
  }

}
