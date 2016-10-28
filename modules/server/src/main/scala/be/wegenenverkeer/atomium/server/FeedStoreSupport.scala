package be.wegenenverkeer.atomium.server

import java.time.OffsetDateTime
import java.util.UUID

import be.wegenenverkeer.atomium.format.{Link, _}

import scala.collection.JavaConverters._

trait FeedStoreSupport[E] {

  val generator = new Generator("atomium", "http://github.com/WegenenVerkeer/atomium", "0.0.1")

  case class FeedParams(feedName: String, baseUrl: Url, title: Option[String])

  /**
    *
    * @param pageSize        the desired feed page size
    * @param entries         the entries to include in the feed
    * @param previousEntryId the previous entry's id or None if we are at the head of the feed
    * @param nextEntryId     the next entry's id or None if we are at the tail of the feed (last page)
    * @return a page feed, possibly empty
    */
  def toFeed(pageSize: Int,
             minId: Long,
             entries: List[FeedStoreSupport[E]#FeedEntry],
             previousEntryId: Option[Long],
             nextEntryId: Option[Long])
            (implicit feedParams: FeedParams): Feed[E] = {
    val selfLink = new Link(Link.SELF, feedLink(nextEntryId.getOrElse(minId), pageSize, forward = true))
    val lastlink = new Link(Link.LAST, feedLink(minId, pageSize, forward = true))
    val nextLink = nextEntryId.map { _ => link(Link.NEXT, entries.last.sequenceNr, pageSize, forward = false) }.toList
    val prevLink = previousEntryId.map { _ => link(Link.PREVIOUS, entries.head.sequenceNr, pageSize, forward = true) }.toList
    val links = List(selfLink, lastlink) ++ prevLink ++ nextLink
    new Feed[E](
      feedParams.feedName,
      feedParams.baseUrl.getPath,
      feedParams.title.getOrElse("<no title>"),
      generator,
      entries.headOption.map(_.entry.getUpdated).getOrElse(OffsetDateTime.now()),
      links.asJava,
      entries.map(_.entry).asJava
    )
  }


  def processForwardEntries(start: Long,
                            pageSize: Int,
                            entries: List[FeedStoreSupport[E]#FeedEntry]): ProcessedFeedEntries = {
    require(entries != Nil)

    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[FeedStoreSupport[E]#FeedEntry] = Nil

    //entries are sorted by id ascending
    if (start == entries.head.sequenceNr) {
      //start should be excluded
      feedEntries = entries.tail.take(pageSize).reverse
      nextId = Some(start) //nextId is start
    } else {
      //there is no next page
      feedEntries = entries.take(pageSize).reverse
    }
    if (feedEntries.nonEmpty && feedEntries.head.sequenceNr != entries.last.sequenceNr)
      previousId = Some(entries.head.sequenceNr)

    ProcessedFeedEntries(previousId, feedEntries, nextId)
  }

  def processBackwardEntries(start: Long,
                             pageSize: Int,
                             entries: List[FeedStoreSupport[E]#FeedEntry]): ProcessedFeedEntries = {
    require(entries != Nil)

    var nextId: Option[Long] = None
    var previousId: Option[Long] = None
    var feedEntries: List[FeedStoreSupport[E]#FeedEntry] = Nil

    //backward => entries are sorted by id descending
    if (start == entries.head.sequenceNr) {
      // exclude start
      feedEntries = entries.tail.take(pageSize)
      previousId = Some(start) //previousId is start
    } else {
      //there is no next page
      feedEntries = entries.take(pageSize)
    }
    if (feedEntries.nonEmpty && feedEntries.last.sequenceNr != entries.last.sequenceNr)
      nextId = Some(entries.last.sequenceNr)

    ProcessedFeedEntries(previousId, feedEntries, nextId)
  }

  def processFeedEntries(start: Long, minId: Long, pageSize: Int, forward: Boolean, entries: List[FeedStoreSupport[E]#FeedEntry])
                        (implicit feedParams: FeedParams): Feed[E] = {
    if (entries.nonEmpty) {
      val result = if (forward) {
        processForwardEntries(start, pageSize, entries)
      } else {
        processBackwardEntries(start, pageSize, entries)
      }
      toFeed(pageSize, minId, result.feedEntries, result.previousSequenceNr, result.nextSequenceNr)
    } else {
      toFeed(pageSize, minId, Nil, None, None)
    }
  }

  //we possibly need to return less entries to keep paging consistent => paging from tail to head or vice versa
  //must return the same pages in order to have efficient caching
  def processHeadFeedEntries(numberOfEntriesLower: Long, minId: Long, pageSize: Int, entries: List[FeedStoreSupport[E]#FeedEntry])
                            (implicit feedParams: FeedParams) = {
    val n = (numberOfEntriesLower % pageSize).toInt
    val limit = if (n == 0) pageSize else n

    toFeed(pageSize, minId, entries.take(limit), None, entries.drop(limit) match {
      case Nil    => None
      case h :: _ => Some(h.sequenceNr)
    })

  }


  def getNextLink(id: Long, count: Int, next: Option[Long]): Option[Link] = {
    next.map { _ =>
      link(Link.NEXT, id, count, forward = false)
    }
  }

  def getPreviousLink(id: Long, count: Int, previous: Option[Long]): Option[Link] = {
    previous.map { _ =>
      link(Link.PREVIOUS, id, count, forward = true)
    }
  }

  protected def link(l: String, start: Long, pageSize: Int, forward: Boolean): Link = {
    new Link(l, feedLink(start, pageSize, forward))
  }

  protected def generateEntryID(): String = {
    s"urn:uuid:${UUID.randomUUID().toString}"
  }

  /**
    * Creates a link to a feed page.
    *
    * @param startId the starting entry's id (non inclusive)
    * @param count   the number of entries in the page
    * @param forward if true navigate to 'previous' elements in feed (towards head of feed)
    *                else navigate to 'next' elements in feed (towards last page of feed)
    * @return the URL
    */
  protected def feedLink(startId: Long, count: Int, forward: Boolean): String = {
    val direction = if (forward) "forward" else "backward"
    new Url(startId.toString).add(direction).add(count.toString).getPath
  }

  case class FeedEntry(sequenceNr: Long, entry: Entry[E])

  case class ProcessedFeedEntries(previousSequenceNr: Option[Long],
                                  feedEntries: List[FeedStoreSupport[E]#FeedEntry],
                                  nextSequenceNr: Option[Long])

}
