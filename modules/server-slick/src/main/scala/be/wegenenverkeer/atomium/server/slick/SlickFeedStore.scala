package be.wegenenverkeer.atomium.server.slick

import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.AbstractFeedStore

/**
 * An [[AbstractFeedStore]] implementation that stores feeds and pages in a SQL database.
 * This implementation requires that the entries table for each feed is explicitly created upfront.
 *
 * @param feedComponent the feedComponent trait to access the driver
 * @param feedName the name of the feed
 * @param title the optional title of the feed
 * @param entriesTableName the name of the table storing the entries for this feed, must be created explicitly
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param url the base `Url`
 * @tparam E type of the elements in the feed
 */
case class SlickFeedStore[E](feedComponent: FeedComponent,
                             feedName: String,
                             title: Option[String],
                             entriesTableName: String,
                             ser: E => String,
                             deser: String => E,
                             url: Url)
                            (implicit context:SlickJdbcContext)
  extends AbstractSlickFeedStore[E](feedName, title, ser, deser, url) {


  import feedComponent.driver.simple._

  protected def getEntryTableQuery: TableQuery[feedComponent.EntryTable] = {
    feedComponent.entriesTableQuery(entriesTableName)
  }

}

