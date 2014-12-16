package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.slick.FeedComponent

/**
 * [[AbstractFeedStore]] implementation that stores feeds and pages in a SQL database.
 * This implementation requires that the entries table for each feed is explicitly created upfront.
 *
 * @param feedComponent the feedComponent trait to access the driver
 * @param context: the context implementation (wraps a session)
 * @param feedName the name of the feed
 * @param title the optional title of the feed
 * @param entriesTableName the name of the table storing the entries for this feed, must be created explicitly
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlBuilder helper to build urls
 * @tparam E type of the elements in the feed
 */
case class ManualJdbcFeedStore[E](feedComponent: FeedComponent,
                       context: JdbcContext,
                       feedName: String,
                       title: Option[String],
                       entriesTableName: String,
                       ser: E => String,
                       deser: String => E,
                       urlBuilder: UrlBuilder) extends AbstractJdbcFeedStore[E](context, feedName, title, ser, deser, urlBuilder) {


  import feedComponent.driver.simple._

  protected def getEntryTableQuery: TableQuery[feedComponent.EntryTable] = {
    feedComponent.entriesTableQuery(entriesTableName)
  }

}

