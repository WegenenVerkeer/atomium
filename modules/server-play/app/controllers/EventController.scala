package controllers

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atomium.japi.format.{Feed => JFeed}
import be.wegenenverkeer.atomium.play.PlayJsonFormats._
import be.wegenenverkeer.atomium.server.play.{FeedSupport, JaxbFeedMarshaller, PlayJsonFeedMarshaller}
import be.wegenenverkeer.atomium.server.{Context, FeedService}
import play.api.mvc.Controller

/**
 * This controller serves pages of a feed containing {{Event}}.
 * It registers both a Play json marshaller and a Jaxb xml marshaller
 * and thus supports content negotiation and can support both xml or json responses
 * @param feedService the feedService used for retrieving feed pages
 */
class EventController(feedService: FeedService[Event, Context]) extends Controller with FeedSupport[Event] {

  implicit val context: Context = new Context {} //dummy context for MemoryFeedStore

  //play json marshaller

  import controllers.EventFormat._

  //jaxb marshaller
  implicit val jaxbContext = JAXBContext.newInstance(classOf[JFeed[Event]], classOf[Event])

  override def marshallers = {
    case Accepts.Xml()  => JaxbFeedMarshaller[Event]()
    case Accepts.Json() => PlayJsonFeedMarshaller[Event]()
  }

  /**
   * @return the head of the page
   */
  def headOfFeed() = {
    processFeedPage(feedService.getHeadOfFeed())
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
