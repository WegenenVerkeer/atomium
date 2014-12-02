package controllers

import javax.xml.bind.JAXBContext

import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.Marshallers._
import be.vlaanderen.awv.atom.Formats._

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
  def getHeadOfFeed() = {
    processFeedPage(feedService.getHeadOfFeed())
  }

  /**
   *
   * @param start
   * @param pageSize
   * @return a page of the feed
   */
  def getFeedPage(start: Int, pageSize: Int) = {
    processFeedPage(feedService.getFeedPage(start, pageSize))
  }

}
