package be.vlaanderen.awv.atom

import scala.slick.driver.H2Driver.simple.{Session,Database}
import JdbcContext._

object SlickSample {
  val feedStoreFactory: (String, JdbcContext) => FeedStore[Int] = (feedName, context) => new JdbcFeedStore[Int](context)
  val feedService = new FeedService[Int, JdbcContext]("int_feed", 100, "int_feed", feedStoreFactory)

  case class Element(i: Int)

  Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withTransaction  {
    implicit session =>
    // do some stuff with session: ElementTable.save ...
    feedService.push(1)(session) // push element to feedservice
  }
}
