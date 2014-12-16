package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.models.FeedModel
import be.wegenenverkeer.atom.slick.FeedComponent

import scala.slick.jdbc.meta.MTable

/**
 * [[AbstractFeedStore]] implementation that stores feeds and pages in a SQL database.
 * This class automatically manages the necessary tables:
 *
 * A FEED table is automatically created if it does not exist, which contains the info for all the feeds.
 * It is safe to create this table upfront if needed.
 *
 * For each feed a FEED_ENTRIES table is automatically created
 * The name of each feeds entries table is "FEED_ENTRIES_i where i is the primary key (sequence number)
 * of the feed from the FEED table
 *
 * @param fc the feedComponent trait to access the driver
 * @param context: the context implementation (wraps a session)
 * @param feedName the name of the feed
 * @param title the optional title of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlBuilder helper to build urls
 * @tparam E type of the elements in the feed
 */
case class AutoJdbcFeedStore[E](feedComponent: FeedComponent,
                       context: JdbcContext,
                       feedName: String,
                       title: Option[String],
                       ser: E => String,
                       deser: String => E,
                       urlBuilder: UrlBuilder) extends AbstractJdbcFeedStore[E](context, feedName, title, ser, deser, urlBuilder) {

  import feedComponent.driver.simple._

  /**
   * auto-registers the feed in FEEDS table and automatically creates the feed's entries table
   * The FeedModel is also cached, so that recreating the AutoJdbcFeedStore does not have to lookup the FeedModel from
   * the database each time.
   */
  lazy val feedModel: FeedModel = {
    FeedModelRegistry.getOrElseUpdate(feedName, {

      implicit val session = context.session

      //create table FEEDS if it does not exist
      if (MTable.getTables("FEEDS").list(session).isEmpty) {
        feedComponent.Feeds.ddl.create
      }

      //check if feedName is found in table FEEDS
      feedComponent.Feeds.findByName(feedName).getOrElse {
        val id = feedComponent.Feeds returning feedComponent.Feeds.map(_.id) += new FeedModel(None, feedName, title)
        val f = new FeedModel(id, feedName, title)
        feedComponent.entriesTableQuery(f.entriesTableName).ddl.create
        f
      }

    })
  }

  override def getEntryTableQuery: TableQuery[feedComponent.EntryTable] = {
    feedComponent.entriesTableQuery(feedModel.entriesTableName)
  }

}

/**
 * FeedModelRegistry keeps a maps of feedName to its corresponding FeedModel
 * in order to avoid redundant queries to the DB to retrieve the FeedModel
 */
object FeedModelRegistry {

  import scala.collection.immutable.Map
  private var feedModelCache: Map[String, FeedModel] = Map.empty[String, FeedModel]

  def getOrElseUpdate(key: String, op: => FeedModel): FeedModel = {
    if (feedModelCache.contains(key)) {
      feedModelCache(key)
    } else {
      val feedModel = op
      feedModelCache += (key -> feedModel)
      feedModel
    }
  }

  def clear() = feedModelCache = Map.empty

}
