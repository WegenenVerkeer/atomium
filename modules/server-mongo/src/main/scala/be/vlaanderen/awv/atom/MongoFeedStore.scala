package be.vlaanderen.awv.atom

import _root_.java.util.UUID

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{DBObject, casbah}
import org.joda.time.LocalDateTime

/**
 * [[be.vlaanderen.awv.atom.FeedStore]] implementation that stores feeds and pages in a MongoDB entriesCollection.
 *
 * @param c the context implementation
 * @param feedEntriesCollectionName name of the entriesCollection that contains the feed entries
 * @param feedInfoCollectionName name of the entriesCollection that contains the feed info of all the feeds
 * @param ser function to serialize an element to a DBObject
 * @param deser function to deserialize a DBObject to an element
 * @param urlProvider
 * @tparam E type of the elements in the feed
 */
class MongoFeedStore[E](c: MongoContext,
                        feedName: String,
                        title: Option[String] = None,
                        feedEntriesCollectionName: Option[String] = None,
                        feedInfoCollectionName: String,
                        ser: E => DBObject, deser: DBObject => E, urlProvider: UrlBuilder) extends FeedStore[E](feedName, title, urlProvider) {
  import be.vlaanderen.awv.atom.MongoFeedStore._

  lazy val context = c

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
    update = $setOnInsert(Keys.Sequence -> 1, Keys._Id -> feedName), //dbo to create if not exists
    remove = false, returnNew = true, upsert = true)


  protected def feedEntry2DbObject(e: E) = MongoDBObject(
    Keys.Uuid -> UUID.randomUUID().toString,
    Keys.Timestamp -> new LocalDateTime(),
    Keys.Content -> ser(e)
  )

  protected def dbObject2FeedEntry(dbo: DBObject): Entry[E]  = {
    val entryDbo = dbo.as[DBObject](Keys.Content)
    Entry(dbo.as[String](Keys.Uuid), dbo.as[LocalDateTime](Keys.Timestamp), Content(deser(entryDbo), ""), Nil)
  }

  /**
   * push a list of entries to the feed
   * @param entries the entries to push to the feed
   */
  override def push(entries: Iterable[E]): Unit =
    entries foreach { entry =>
      entriesCollection.update(MongoDBObject(Keys._Id -> getNextSequence),
        feedEntry2DbObject(entry),
        upsert = false, multi = false, WriteConcern.Safe)
    }

  /**
   * return pageSize entries starting from start
   * @param start the start entry
   * @param pageSize the number of entries to return
   * @return
   */
  override def getFeedEntries(start: Int, pageSize: Int): List[Entry[E]] = {
    entriesCollection.find().sort(MongoDBObject(Keys.Sequence -> -1)).drop(start).take(pageSize).toList.reverse.map(dbObject2FeedEntry)
  }

  @annotation.tailrec
  private def retry[T](n: Int)(fn: => Option[T]): T = {
    if (n == 0) throw new Exception("")
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
      update = $inc("seq" -> 1), //increment seq by 1
      remove = false, returnNew = true, upsert = false)

    retry(5)(_getNextSequence).get(Keys.Sequence).asInstanceOf[Long]
  }

  def maxId: Long = {
    entriesCollection.find().sort(MongoDBObject(Keys.Sequence -> -1)).limit(1).toList match {
      case h :: Nil => h.get(Keys.Sequence).asInstanceOf[Long]
      case _ => 0
    }
  }

}

object MongoFeedStore {
  object Keys {
    lazy val _Id = "_id"
    lazy val Sequence = "seq"
    lazy val Uuid = "uuid"
    lazy val Timestamp = "timestamp"
    lazy val Content = "content"

    lazy val Feed = "feed"
    lazy val Count = "count"
    lazy val LastPage = "last_page"
  }
}
