package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.slick.FeedDAL

import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple.Database

object SlickSample {

  val db = Database.forURL("jdbc:h2:mem:test1", driver = "org.h2.Driver")
  val feedDAL: FeedDAL = new FeedDAL(H2Driver)

  def feedStoreFactory: (String, JdbcContext) => AbstractFeedStore[String] = (feedName, context) =>
    new AutoJdbcFeedStore[String](feedDAL, context, "int_feed", None, null, null, null)

  val feedService = new FeedService[String, JdbcContext]("int_feed", 100, feedStoreFactory)

  case class Element(i: Int)

  db withTransaction  {
    implicit session =>
    implicit val context = feedDAL.createJdbcContext(session)
    // do some stuff with session: ElementTable.save ...
    feedService.push("1") // push element to feedservice
  }
}
