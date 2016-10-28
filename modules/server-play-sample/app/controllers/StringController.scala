package controllers

import be.wegenenverkeer.atomium.server.play.{FeedMarshaller, FeedSupport, JacksonFeedMarshaller}
import be.wegenenverkeer.atomium.server.{Context, FeedService}
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import play.api.mvc.Controller

import scala.concurrent.Future

/**
 * This controller serves pages of a feed containing Strings.
 * It only supports a Jackson json marshaller and thus only supports JSON responses
 */
class StringController() extends Controller with FeedSupport[String] {
  
  lazy val feedService = Deps.stringService

  implicit val context: Context = new Context {} //dummy context for MemoryFeedStore

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(new JSR310Module())
  objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  implicit val objectWriter: ObjectWriter = objectMapper.writer()

  override def marshallers = {
    case Accepts.Xml() => JacksonFeedMarshaller[String]()
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
