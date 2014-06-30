package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.models._
import slick.SlickPostgresDriver.simple._

/**
 * [[be.vlaanderen.awv.atom.FeedStore]] implementation that stores feeds and pages in a Postgres database.
 *
 * @param c the context implementation
 * @param feedName the name of the feed
 * @param ser function to serialize an element to a String
 * @param deser function to deserialize a String to an element
 * @param urlProvider
 * @tparam E type of the elements in the feed
 */
class JdbcFeedStore[E](c: JdbcContext, feedName: String, ser: E => String, deser: String => E, urlProvider: UrlBuilder) extends FeedStore[E] {

  lazy val context = c

  protected def feedLink(value: Option[Long], linkType: String) = {
    value map { v =>
      Link(linkType, urlProvider.feedLink(v))
    } toList
  }

  /**
   * Retrieves a page of the feed.
   *
   * @param page the page number
   * @return the feed page or `None` if the page is not found
   */
  override def getFeed(page: Long): Option[Feed[E]] = {
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session] // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    def eventQuery(feedId: Long) = for {
      fe <- EntryTable if fe.feedName === feedName && fe.feedId === feedId
    } yield fe
    for {
      feed <- FeedTable.findById(page, feedName)
      entries <- Some(eventQuery(feed.id).list)
    } yield Feed(
      id = feed.id.toString,
      base = urlProvider.base,
      title = feed.title,
      updated = feed.timestamp.toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"),
      links =
        feedLink(Some(feed.id), Link.selfLink) ++
        feedLink(Some(feed.first), Link.firstLink) ++
        feedLink(feed.next, Link.nextLink) ++
        feedLink(feed.previous, Link.previousLink),
      entries = entries map { entry =>
        Entry(Content(List(deser(entry.value)), ""), Nil)
      }
    )
  }

  /**
   * Updates the feed pages and feed info.
   *
   * @param feedUpdates
   * @param feedInfo
   */
  override def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo): Unit = {
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session] // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    feedUpdates foreach { feedUpdate =>
      val feedModel = FeedModel(
        id = feedUpdate.page,
        name = feedName,
        title = Some(feedUpdate.title),
        timestamp = feedUpdate.updated.toLocalDateTime,
        first = feedUpdate.first,
        previous = feedUpdate.previous,
        next = feedUpdate.next
      )

      // create new feed or update existing feed
      if (feedUpdate.isNew) {
        FeedTable += feedModel
      } else {
        FeedTable.updateFeed(feedModel)
      }

      // insert new entries
      feedUpdate.newElements foreach { element =>
        val entry = EntryModel(
          feedId = feedUpdate.page,
          feedName = feedName,
          value = ser(element)
        )
        EntryTable += entry
      }

      // save current feed info object
      val feedInfoModel = FeedInfoModel(
        feedName = feedName,
        lastPageId = feedInfo.lastPage,
        count = feedInfo.count
      )

      FeedInfoTable += feedInfoModel
    }
  }

  /**
   * Gets the feed info.
   *
   * @return the feed info or None if the feed is not persisted yet
   */
  override def getFeedInfo: Option[FeedInfo] = {
    implicit val session = c.session.asInstanceOf[be.vlaanderen.awv.atom.slick.SlickPostgresDriver.simple.Session] // TODO hack, moet opgelost worden wanneer we een generieke oplossing hebben voor slick profiles
    for {
      fim <- FeedInfoTable.findLastByFeedName(feedName)
    } yield FeedInfo(
      count = fim.count,
      lastPage = fim.lastPageId
    )
  }
}
