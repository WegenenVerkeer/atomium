package be.wegenenverkeer.atom

import resource._

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
 * A feed processor fetches pages from the feed and offers the new items to the entry consumer.
 *
 * The processor decides which feed page to fetch and which items are considered as new items based on the initial feed
 * position.
 *
 * The processor uses the feed provider to fetch the feed pages.
 *
 * @param feedProvider the feed provider is responsible for fetching the feed pages
 * @param entryConsumer the entry consumer is responsible to consume the new entries
 *
 * @tparam E the type of the entries in the feed
 */
class FeedProcessor[E](feedProvider: FeedPageProvider[E],
                       entryConsumer: EntryConsumer[E]) {

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

  /**
   * Start the consuming of Feeds. Returns a {{{Try[Unit]}}}.
   * In case of success a Success[Unit]
   * or in case of failure a Failure[FeedProcessingException]
   *
   * TODO: we should find a better way for doing this. Eventually two method, start() and startManaged().
   * @return Try[Unit].
   *
   */
  def start(): Try[Unit] = {
    // MOET WEG

    // Bloody hack: Scala-ARM needs a Manifest for the managed resource,
    // but we don't want to pollute the interface if it, because we intend to use it from Java as well
    implicit val manif = new Manifest[FeedPageProvider[E]] {
      override def runtimeClass: Class[_] = classOf[FeedPageProvider[E]]
    }
    implicit val feedProvResource = FeedPageProvider.managedFeedProvider(feedProvider)

    val extResult = managed(feedProvider).map { provider =>
      // FeedProvider must be managed
      initEventCursor flatMap process
    }

    extResult.either match {
      case Left(throwables) =>
        val messages = throwables.map(_.getMessage).mkString("[", "] | [", "[")
        Failure(FeedProcessingException(None, messages))
      case Right(Failure(ex)) => Failure(ex)
      case Right(_) => Success()

    }
  }

  private def initEventCursor: Try[EntryCursor] = {
    feedProvider.fetchFeed().map {
      feed => buildCursor(feed, feedProvider.initialPosition)
    }
  }


  @tailrec
  private def process(cursor: EntryCursor): FeedProcessingResult = {

    //!!! NOTE !!!
    // we perform two times the same pattern matching on a Try[EventCursor]
    // we need to do this otherwise we can't have tail call optimization and we absolutely need tailrec here
    cursor match {
      case currentEvent: EntryPointer =>

        val nextEventOrFailure = for {
          _ <- consumeEvent(currentEvent)
          nextEvent <- buildNextEntryCursor(currentEvent)
        } yield nextEvent

        // fetch nextEvent cursor:
        // next EventCursor could be an EndOfEntries or an EntryOnNextPage.
        // in case of EntryOnPreviousFeedPage we could fail to retrieve the previous page of the feed.
        // in case of Success we process the next EventCursor
        // mapping on Try does not work here, because of tailrec
        nextEventOrFailure match {
          case Success(next) => process(next)
          case Failure(ex) => Failure(ex)
        }

      // map on Try does not work here, because of tailrec
      case EntryOnPreviousFeedPage(previousFeedUrl) =>
        cursorOnPreviousFeedPage(previousFeedUrl) match {
          case Success(next) => process(next)
          case Failure(ex) => Failure(ex)
        }

      case EndOfEntries(lastFeedPos) => Success() // we are done
    }

  }

  private def consumeEvent(currentEvent: EntryPointer): FeedProcessingResult = {
    try {
      entryConsumer.apply(currentEvent.feedPosition, currentEvent.entryToProcess)
    } catch {
      case ex: Exception =>
        Failure(ex)
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

    def buildCursorWithPositionOn(feed: Feed[EntryType], index: Int) = {
      EntryPointer(
        feed.entries.head,
        feed.entries.tail,
        FeedPosition(feed.resolveUrl(feed.selfLink.href), index),
        feed
      )
    }

    def previousFeedOrEnd(feed: Feed[EntryType]) = {
      // we go to previous feed page or we reached the EndOfEntries
      def endOfEntries = {
        feedPosition match {
          case Some(feedPos) => EndOfEntries(feedPos)
          case None =>
            // rather exceptional situation, can only occurs if a feed is completely empty
            EndOfEntries(FeedPosition(feed.resolveUrl(feed.selfLink.href), 0))
        }
      }

      feed.previousLink match {
        case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href))
        case None => endOfEntries
      }

    }

    if (feed.entries.nonEmpty) {
      feedPosition match {
        // no FeedPosition means start of new page
        case None => buildCursorWithPositionOn(feed, 0)
        case Some(feedPos) =>
          // are there still entries to process?
          val nextIndex = feedPos.index + 1
          val remainingEntries = feed.entries.drop(nextIndex)
          remainingEntries match {
            case Nil => previousFeedOrEnd(feed)
            case _ =>
              val partialFeed = feed.copy(entries = remainingEntries)
              buildCursorWithPositionOn(partialFeed, nextIndex)
          }
      }
    } else {
      previousFeedOrEnd(feed)
    }
  }

  private def cursorOnPreviousFeedPage(url: Url): Try[EntryCursor] = {
    feedProvider.fetchFeed(url.path).map { feed => buildCursor(feed)}
  }


  private def buildNextEntryCursor(entryPointer: EntryPointer): Try[EntryCursor] = {
    val nextCursor =
      if (entryPointer.stillToProcessEntries.nonEmpty) {
        entryPointer.copy(
          entryToProcess = entryPointer.stillToProcessEntries.head,
          stillToProcessEntries = entryPointer.stillToProcessEntries.tail, // moving forward, dropping head
          feedPosition = entryPointer.feedPosition.copy(index = entryPointer.feedPosition.index + 1) // moving position forward
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
      case end@EndOfEntries(_) => Success(end)
      // wrap nextCursor in Success
      case _ => Success(nextCursor)

    }
  }

}
