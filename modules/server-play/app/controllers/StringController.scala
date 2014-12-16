package controllers

import be.wegenenverkeer.atom._
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import play.api.http.MimeTypes
import play.api.mvc.Controller

/**
 * This controller serves pages of a feed containing Strings.
 * It only registers a Jackson json marshaller and thus only supports JSON responses

 * @param feedService the feedService used for retrieving feed pages
 */
class StringController(feedService: FeedService[String, Context]) extends Controller with FeedSupport[String] {

  implicit val c: Context = new Context {} //dummy context for MemoryFeedStore

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(new JodaModule)
  objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  implicit val objectWriter = objectMapper.writer()
  registerMarshaller(MimeTypes.JSON, JFeedConverters.feed2JFeed[String] _ andThen JacksonSupport.jacksonMarshaller)

  /**
   * @return the head of the feed
   */
  def headOfFeed() = {
    processFeedPage(feedService.getHeadOfFeed())
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
