package be.wegenenverkeer.atomium.server.mongo

import be.wegenenverkeer.atomium.format.{Content, Entry}
import be.wegenenverkeer.atomium.server.mongo.MongoFeedStore.Keys
import be.wegenenverkeer.atomium.server.{AbstractFeedStore, UrlBuilder}
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{DBObject, casbah}
import org.joda.time.DateTime

/**
 * [[be.wegenenverkeer.atomium.server.AbstractFeedStore]] implementation that stores feeds and pages in a MongoDB entriesCollection.
 *
 * @param c the context implementation
 * @param feedEntriesCollectionName name of the entriesCollection that contains the feed entries
 * @param feedInfoCollectionName name of the entriesCollection that contains the feed info of all the feeds
 * @param ser function to serialize an element to a DBObject
 * @param deser function to deserialize a DBObject to an element
 * @param urlProvider a `UrlBuilder`
 * @tparam E type of the elements in the feed
 */
class MongoFeedStore[E](c: MongoContext,
                        feedName: String,
                        title: Option[String] = None,
                        feedEntriesCollectionName: Option[String] = None,
                        feedInfoCollectionName: String,
                        ser: E => DBObject, deser: DBObject => E, urlProvider: UrlBuilder) extends AbstractFeedStore[E](feedName, title, urlProvider) {

  private lazy val db = c.db.asScala
  private lazy val entriesCollection = db(feedEntriesCollectionName match {
    case Some(name) => name
    case None => feedName
  })
  private lazy val feedInfoCollection = db(feedInfoCollectionName)

  //insert feed into to feedInfoCollection if it does not exist yet
  feedInfoCollection.findAndModify(
    query = MongoDBObject(Keys._Id -> feedName), //where _id = feedName
    fields = MongoDBObject(), //return all elements
    sort = MongoDBObject(), //no sorting
    update = $setOnInsert(Keys.Sequence -> 0), //create with seq -> 1 if not exists
    remove = false, returnNew = true, upsert = true)

  protected def feedEntry2DbObject(e: E): MongoDBObject = feedEntry2DbObject(generateEntryID(), e)

  protected def feedEntry2DbObject(uuid: String, e: E): MongoDBObject = MongoDBObject(
    Keys.Uuid -> uuid,
    Keys.Timestamp -> new DateTime(),
    Keys.Content -> ser(e)
  )

  protected def dbObject2FeedEntry(dbo: DBObject): FeedEntry  = {
    val entryDbo = dbo.as[DBObject](Keys.Content)
    FeedEntry(dbo.as[Long](Keys._Id),
          Entry(id = dbo.as[String](Keys.Uuid),
            updated = dbo.as[DateTime](Keys.Timestamp).toDateTime,
            content = Content(deser(entryDbo), ""), Nil))
  }

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]): Unit = {
    entries foreach {entry =>
      entriesCollection.insert(feedEntry2DbObject(entry) ++ (Keys._Id -> getNextSequence), WriteConcern.Safe)
    }
  }

  override def push(uuid: String, entry: E): Unit = {
    entriesCollection.insert(feedEntry2DbObject(uuid, entry) ++ (Keys._Id -> getNextSequence), WriteConcern.Safe)
  }

  /**
   * Retrieves entries with their sequence numbers from the feed
   *
   * @param start the starting entry (inclusive), MUST be returned in the entries
   * @param count the number of entries to return
   * @param ascending if true return entries with sequence numbers >= start in ascending order
   *                else return entries with sequence numbers <= start in descending order
   * @return the corresponding entries sorted accordingly
   */
  override def getFeedEntries(start: Long, count: Int, ascending: Boolean): List[FeedEntry] = {
    val query = if (ascending)
      entriesCollection.find(Keys._Id $gte start).sort(MongoDBObject(Keys._Id -> 1))
    else
      entriesCollection.find(Keys._Id $lte start).sort(MongoDBObject(Keys._Id -> -1))

    query.take(count).toList.map(dbObject2FeedEntry)
  }

  /**
   * retrieves the most recent entries from the feedstore sorted in descending order
   * @param count the amount of recent entries to return
   * @return a list containing tuples of a sequence number and its corresponding entry
   *         and sorted by descending sequence number
   */
  override def getMostRecentFeedEntries(count: Int): List[FeedEntry] = {
    entriesCollection.find().sort(MongoDBObject(Keys._Id -> -1)).take(count).toList.map(dbObject2FeedEntry)
  }

  /**
   * @param sequenceNr sequence number to match
   * @return the number of entries in the feed with sequence number equal or lower than specified
   */
  override def getNumberOfEntriesLowerThan(sequenceNr: Long, inclusive: Boolean = true): Long = {

    val find = if (inclusive)
      entriesCollection.find(Keys._Id $lte sequenceNr)
    else
      entriesCollection.find(Keys._Id $lt sequenceNr)

    find.sort(MongoDBObject(Keys._Id -> -1)).count()
  }



  @annotation.tailrec
  private def retry[T](n: Int)(fn: => Option[T]): T = {
    if (n == 0) throw new Exception("could not retrieve next sequence number after retrying")
    fn match {
      case Some(x) => x
      case None => retry(n - 1)(fn)
    }
  }

  protected def getNextSequence: Long = {
    def _getNextSequence: Option[casbah.MongoCollection#T] = feedInfoCollection.findAndModify(
      query = MongoDBObject(Keys._Id -> feedName), //where _id = feedName
      fields = MongoDBObject(Keys.Sequence -> 1), //only return seq
      sort = MongoDBObject(), //no sorting
      update = $inc(Keys.Sequence -> 1), //increment seq by 1
      remove = false, returnNew = true, upsert = false)

    retry(5)(_getNextSequence).as[Int](Keys.Sequence).toLong
  }

  override val minId: Long = 0L

  def maxId: Long = {
    entriesCollection.find().sort(MongoDBObject(Keys._Id -> -1)).limit(1).toList match {
      case h :: Nil => h.as[Long](Keys._Id)
      case _ => 0L
    }
  }

}

object MongoFeedStore {
  object Keys {
    val _Id = "_id"
    val Sequence = "seq"
    val Uuid = "uuid"
    val Timestamp = "timestamp"
    val Content = "content"
  }
}
