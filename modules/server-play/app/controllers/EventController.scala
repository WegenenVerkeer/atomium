package controllers

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atom.Formats._
import be.wegenenverkeer.atom.Marshallers._
import be.wegenenverkeer.atom._
import be.wegenenverkeer.atom.java.{Feed => JFeed}
import play.api.mvc.Controller
import support.{JaxbSupport, PlayJsonSupport}

class EventController(feedService: FeedService[Event, Context]) extends Controller with FeedSupport[Event] {

  implicit val c: Context = new Context {} //dummy context for MemoryFeedStore

  //play json marshaller
  import controllers.EventFormat._
  val jsonMarshaller: JsonMarshaller[Feed[Event]] = PlayJsonSupport.jsonMarshaller[Feed[Event]]

  //jaxb marshaller
  implicit val jaxbContext = JAXBContext.newInstance(classOf[JFeed[Event]], classOf[Event])
  val xmlMarshaller: XmlMarshaller[Feed[Event]] = JFeedConverters.feed2JFeed[Event] _ andThen JaxbSupport.jaxbMarshaller

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
  def getFeedPage(start: Int, pageSize: Int) = {
    processFeedPage(feedService.getFeedPage(start, pageSize))
  }

}
