package controllers

import be.vlaanderen.awv.atom.{Context, FeedService}
import com.sun.jersey.api.json.{JSONConfiguration, JSONJAXBContext}
import play.api.mvc.Controller

class MyFeedController(feedService: FeedService[String, Context]) extends Controller with FeedSupport[String] {

  implicit val c: Context = new Context {} //dummy context for MemoryFeedStore

  val config = JSONConfiguration.natural().rootUnwrapping(true).build()
  implicit val jsonJaxbContext = new JSONJAXBContext(config, "be.vlaanderen.awv.atom.jformat")

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
