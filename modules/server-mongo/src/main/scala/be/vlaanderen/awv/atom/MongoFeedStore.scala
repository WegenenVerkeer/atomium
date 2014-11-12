package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.format.{Link, Feed, Entry, Content}
import com.mongodb.MongoClient
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

/**
 * [[be.vlaanderen.awv.atom.FeedStore]] implementation that stores feeds and pages in a MongoDB collection.
 *
 * @param c the context implementation
 * @param collectionName name of the collection that contains the feed pages
 * @param feedInfoCollectionName name of the collection that contains the feed info
 * @param ser function to serialize an element to a DBObject
 * @param deser function to deserialize a DBObject to an element
 * @param urlProvider
 * @tparam E type of the elements in the feed
 */
class MongoFeedStore[E](c: MongoContext, collectionName: String, feedInfoCollectionName: String, ser: E => DBObject, deser: DBObject => E, urlProvider: UrlBuilder) extends FeedStore[E] {
  import MongoFeedStore._

  lazy val context = c

  private lazy val db = c.db.asScala
  private lazy val collection = db(collectionName)
  private lazy val feedInfoCollection = db(feedInfoCollectionName)

  protected def feedUpdate2DbObject(updateInfo: FeedUpdateInfo[E]) = MongoDBObject(
    Keys.Page -> updateInfo.page,
    Keys.FirstPage -> updateInfo.first,
    Keys.Title -> updateInfo.title,
    Keys.Updated -> updateInfo.updated,
    Keys.Elements -> MongoDBList((updateInfo.newElements map ser):_*),
    Keys.PreviousPage -> (updateInfo.previous getOrElse null),
    Keys.NextPage -> (updateInfo.next getOrElse null)
  )

    protected def feedLink(value: Option[Long], linkType: String) = {
    value map { v =>
      Link(linkType, urlProvider.feedLink(v))
    } toList
  }

  protected def dbObject2Feed(dbo: DBObject) = Feed[E](
    id = dbo.as[Long](Keys.Page).toString,
    base = urlProvider.base,
    title = dbo.getAs[String](Keys.Title),
    updated = dbo.as[DateTime](Keys.Updated).toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"),
    links =
      feedLink(Some(dbo.as[Long](Keys.Page)), Link.selfLink) ++
      feedLink(dbo.getAs[Long](Keys.FirstPage), Link.firstLink) ++
      feedLink(dbo.getAs[Long](Keys.NextPage), Link.nextLink) ++
      feedLink(dbo.getAs[Long](Keys.PreviousPage), Link.previousLink),
    entries = dbo.getAsOrElse[MongoDBList](Keys.Elements, MongoDBList()) map { entry =>
      val entryDbo = entry.asInstanceOf[DBObject]
      Entry(Content(List(deser(entryDbo)), ""), Nil)
    } toList
  )

  protected def feedInfo2DbObject(updateInfo: FeedInfo) = MongoDBObject(
    Keys.Feed -> collectionName,
    Keys.Count -> updateInfo.count,
    Keys.LastPage -> updateInfo.lastPage
  )

  protected def dbObject2FeedInfo(dbo: DBObject) = FeedInfo(
    count =  dbo.as[Int](Keys.Count),
    lastPage = dbo.as[Long](Keys.LastPage)
  )


  /**
   * TODO
   *
   * @param feedUpdates
   * @param feedInfo
   */
  override def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo): Unit = {
    feedUpdates foreach { feedUpdate =>
      if (feedUpdate.isNew) {
        val dbo = feedUpdate2DbObject(feedUpdate)
        collection.update(
          MongoDBObject(Keys.Page -> feedUpdate.page),
          dbo,
          upsert = true,
          multi = false,
          concern = WriteConcern.Safe)
      } else {
        val dbo =
          $set(Keys.NextPage -> (feedUpdate.next getOrElse null)) ++
          $pushAll(Keys.Elements -> MongoDBList((feedUpdate.newElements map ser):_*))
        collection.update(
          MongoDBObject(Keys.Page -> feedUpdate.page),
          dbo,
          upsert = false,
          multi = false,
          concern = WriteConcern.Safe)
      }
    }

    feedInfoCollection.update(
      MongoDBObject(Keys.Feed -> collectionName),
      feedInfo2DbObject(feedInfo),
      upsert = true,
      multi = false,
      concern = WriteConcern.Safe
    )
  }

  override def getFeedInfo: Option[FeedInfo] = {
    feedInfoCollection.findOne(MongoDBObject(Keys.Feed -> collectionName)) map dbObject2FeedInfo
  }

  override def getFeed(page: Long): Option[Feed[E]] = {
    val feedInfo = getFeedInfo
    val feedOpt: Option[Feed[E]] = collection findOne(MongoDBObject(Keys.Page -> page)) map dbObject2Feed
    feedOpt
  }
}

object MongoFeedStore {
  object Keys {
    lazy val Page = "page"
    lazy val FirstPage = "first"
    lazy val PreviousPage = "previous"
    lazy val NextPage = "next"
    lazy val Title = "title"
    lazy val Updated = "updated"
    lazy val Elements = "elements"

    lazy val Feed = "feed"
    lazy val Count = "count"
    lazy val LastPage = "last_page"
  }
}
