package be.wegenenverkeer.atom

import scala.util.{Failure, Success, Try}

class FeedEntryIterator[E] (feedProvider: FeedPageProvider[E]) extends Iterator[Try[Entry[E]]] {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  /** Internal pointer to the current entry. */
  trait EntryCursor

  case class EntryPointer(entryToProcess: Entry[EntryType],
                          stillToProcessEntries: Entries,
                          feedPosition: FeedPosition,
                          feed: Feed[EntryType]) extends EntryCursor

  case class EntryOnPreviousFeedPage(previousFeedUrl: Url) extends EntryCursor

  case class EndOfEntries(lastFeedPosition: FeedPosition) extends EntryCursor

  private var cursor: Try[EntryCursor] = initEventCursor



  override def hasNext: Boolean = {
    cursor match {
      case Success(_: EntryPointer) => true
      case _ => false
    }
  }

  override def next(): Try[Entry[E]] = {

    cursor match {
      case Success(entryPointer:EntryPointer) =>
        cursor = buildNextEntryCursor(entryPointer)
        Try(entryPointer.entryToProcess)

      case Success(_) => Failure(new NoSuchElementException("No new entries available!"))

      case Failure(e) => Failure(e)
    }
  }



  private def initEventCursor: Try[EntryCursor] = {
    feedProvider.fetchFeed().map {
      feed => buildCursor(feed, feedProvider.initialPosition)
    }
  }


  /**
   * Build an EventCursor according to the following rules:
   *
   * <ul>
   * <li>If there is no FeedPosition => create a new cursor on a full Feed with index 0</li>
   * <li>If there is a valid FeedPosition => increase index and drop entries preceding new index and create new cursor.</li>
   * <li>If the last element of this page is already consumed
   * <ul>
   * <li>If there is 'previous' Link  create an EntryOnPreviousFeedPage cursor on the next page of the feed</li>
   * <li>If there is no 'previous' Link then create and EndOfEntries cursor.</li>
   * </ul>
   * </li>
   * </ul>
   *
   */
  private def buildCursor(feed: Feed[EntryType], feedPosition: Option[FeedPosition] = None): EntryCursor = {

    def buildEntryPointer(feed: Feed[EntryType]) : EntryPointer = {
      val entryId = feed.entries.headOption.map(_.id)
      EntryPointer(
        feed.entries.head,
        feed.entries.tail,
        FeedPosition(feed.resolveUrl(feed.selfLink.href), entryId),
        feed
      )
    }

    def previousFeedOrEnd(feed: Feed[EntryType]): EntryCursor = {
      // we go to previous feed page or we reached the EndOfEntries
      def endOfEntries = {
        feedPosition match {
          case Some(feedPos) => EndOfEntries(feedPos)
          case None =>
            // rather exceptional situation, can only occurs if a feed is completely empty
            EndOfEntries(FeedPosition(feed.resolveUrl(feed.selfLink.href), None))
        }
      }

      feed.previousLink match {
        case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href))
        case None => endOfEntries
      }

    }


      /** Drop entries from feed page up to the current entry id */
    def dropEntriesUpToFeedPos(entries: List[Entry[E]] ) : Feed[EntryType] = {
      val entryId = feedPosition.flatMap(_.entryId)

      val remainingEntries =
        entryId match {
          case Some(id) =>
            val entriesUpToPos = entries.dropWhile(_.id == id)
            // if non-empty list we want the tail only
            // the head is last successful consumed entry
            entriesUpToPos match {
              case x :: xs => xs
              case _ => Nil
            }

          // no entryId means start of new page
          case None => entries
        }
      feed.copy(entries = remainingEntries)
    }

    val reducedFeedPage = dropEntriesUpToFeedPos(feed.entries)


    if (reducedFeedPage.entries.nonEmpty)
      buildEntryPointer(reducedFeedPage)
    else
      previousFeedOrEnd(reducedFeedPage)

  }


  private def cursorOnPreviousFeedPage(url: Url): Try[EntryCursor] = {
    feedProvider.fetchFeed(url.path).map { feed => buildCursor(feed)}
  }


  private def buildNextEntryCursor(entryPointer: EntryPointer): Try[EntryCursor] = {

    val nextCursor = if (entryPointer.stillToProcessEntries.nonEmpty) {

        val nextEntryId = entryPointer.stillToProcessEntries.headOption.map(_.id)

        entryPointer.copy(
          entryToProcess = entryPointer.stillToProcessEntries.head,
          stillToProcessEntries = entryPointer.stillToProcessEntries.tail, // moving forward, dropping head
          feedPosition = entryPointer.feedPosition.copy(entryId = nextEntryId) // moving position forward
        )

      } else {
        entryPointer.feed.previousLink match {
          case None => EndOfEntries(entryPointer.feedPosition)
          case Some(link) => EntryOnPreviousFeedPage(entryPointer.feed.resolveUrl(link.href))
        }
      }

    nextCursor match {
      // still a page to go? go fetch it
      case EntryOnPreviousFeedPage(previousFeedUrl) => cursorOnPreviousFeedPage(previousFeedUrl)
      // no next feed link? stop processing, all entries were consumed
      case end : EndOfEntries => Success(end)
      // wrap nextCursor in Success
      case _ => Success(nextCursor)

    }
  }
}
