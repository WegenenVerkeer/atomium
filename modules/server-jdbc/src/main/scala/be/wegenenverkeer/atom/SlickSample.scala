package be.wegenenverkeer.atom

import scala.slick.driver.H2Driver.simple.Database

object SlickSample {
  val feedStoreFactory: (String, JdbcContext) => AbstractFeedStore[String] = (feedName, context) => new JdbcFeedStore[String](context, "int_feed", None, null, null, null)
  val feedService = new FeedService[String, JdbcContext]("int_feed", 100, feedStoreFactory)

  case class Element(i: Int)

  Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver") withTransaction  {
    implicit session =>
    // do some stuff with session: ElementTable.save ...
    feedService.push("1")(session) // push element to feedservice
  }
}
