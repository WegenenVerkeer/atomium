package controllers

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atom.Marshallers._
import be.wegenenverkeer.atom._
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.joda.JodaModule
import play.api.mvc.Controller
import support.{JacksonSupport, JaxbSupport}

class StringController(feedService: FeedService[String, Context]) extends Controller with FeedSupport[String] {

  implicit val c: Context = new Context {} //dummy context for MemoryFeedStore

  val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(new JodaModule)
  objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
  implicit val objectWriter = objectMapper.writer()
  val jsonMarshaller: JsonMarshaller[Feed[String]] = JFeedConverters.feed2JFeed[String] _ andThen JacksonSupport.jacksonMarshaller

  implicit val jaxbContext = JAXBContext.newInstance("be.wegenenverkeer.atom.java")
  val xmlMarshaller: XmlMarshaller[Feed[String]] = JFeedConverters.feed2JFeed[String] _ andThen JaxbSupport.jaxbMarshaller

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
