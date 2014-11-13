package be.vlaanderen.awv.atom

import scala.annotation.tailrec
import resource._

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
class FeedProcessor[E](feedProvider: FeedProvider[E],
                       entryConsumer: EntryConsumer[E]) {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  trait EventCursor
  case class EntryPointer(entryToProcess: Entry[EntryType],
                          stillToProcessEntries: Entries,
                          feedPosition:FeedPosition,
                          feed:Feed[EntryType]) extends EventCursor

<<<<<<< HEAD
  case class EntryOnNextPage(nextPageUrl:Link) extends EventCursor
=======
  case class EntryOnPreviousFeed(previousFeedUrl:Link) extends EventCursor
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
  case class EndOfEntries(lastFeedPosition:FeedPosition) extends EventCursor


  /**
   * Start the consuming of Feeds. Returns a {{{Try[Unit]}}}.
   * In case of success a Success[Unit]
   * or in case of failure a Failure[FeedProcessingException]
   *
   * TODO: we should find a better way for doing this. Eventually two method, start() and startManaged().
   * @return Try[Unit].
   *
   */
  def start() : Try[Unit] = {

    // Bloody hack: Scala-ARM needs a Manifest for the managed resource,
    // but we don't want to pollute the interface if it, because we intend to use it from Java as well
    implicit val manif  = new Manifest[FeedProvider[E]] {
      override def runtimeClass: Class[_] = classOf[FeedProvider[E]]
    }
    implicit val feedProvResource = FeedProvider.managedFeedProvider(feedProvider)

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

  private def initEventCursor : Try[EventCursor] = {
    feedProvider.fetchFeed().map {
      feed => buildCursor(feed, feedProvider.initialPosition)
    }
  }


  @tailrec
  private def process(cursor:EventCursor) : FeedProcessingResult = {

    //!!! NOTE !!!
    // we perform two times the same pattern matching on a Try[EventCursor]
    // we need to do this otherwise we can't have tail call optimization and we absolutely need tailrec here
    cursor match {
      case currentEvent:EntryPointer =>

        val nextEventOrFailure = for {
          _ <- consumeEvent(currentEvent)
          nextEvent <- buildNextEntryCursor(currentEvent)
        } yield nextEvent

        // fetch nextEvent cursor:
        // next EventCursor could be an EndOfEntries or an EntryOnNextPage.
        // in case of EntryOnNextPage we could fail to retrive the next page of the feed.
        // in case of Success we process the next EventCursor
        // mapping on Try does not work here, because of tailrec
        nextEventOrFailure match {
          case Success(next) => process(next)
          case Failure(ex) => Failure(ex)
        }

      // map on Try does not work here, because of tailrec
<<<<<<< HEAD
      case EntryOnNextPage(nextPageUrl) =>
        cursorOnNextPage(nextPageUrl) match {
=======
      case EntryOnPreviousFeed(previousFeedUrl) =>
        cursorOnPreviousFeed(previousFeedUrl) match {
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
          case Success(next) => process(next)
          case Failure(ex) => Failure(ex)
        }

      case EndOfEntries(lastFeedPos) => Success() // we are done
    }

  }

  private def consumeEvent(currentEvent:EntryPointer) : FeedProcessingResult = {
    try {
      entryConsumer.apply(currentEvent.feedPosition, currentEvent.entryToProcess)
    } catch {
      case ex:Exception =>
        Failure(ex)
    }
  }

  /**
   * Build an EventCursor according to the following rules:
   *
   * <ul>
   *  <li>If there is no FeedPosition => create a new cursor on a full Feed with index 0</li>
   *  <li>If there is a valid FeedPosition => increase index and drop entries preceeding new index and create new cursor.</li>
   *  <li>If the last element of this page is already consumed
   *    <ul>
<<<<<<< HEAD
   *      <li>If there is 'next' Link  create an EntryOnNextPage cursor on the next page of the feed</li>
   *      <li>If there is no 'next' Link then create and EndOfEntries cursor.</li>
=======
   *      <li>Ga naar de vorige feed (more recent) als een Link 'previous' bestaat. Maak een EventOnPreviousFeed cursor.</li>
   *      <li>Is er geen Link naar een volgende Feed? Dan maak een EndOfEvents cursor.</li>
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
   *    </ul>
   *  </li>
   * </ul>
   *
   */
  private def buildCursor(feed:Feed[EntryType], feedPosition:Option[FeedPosition] = None) : EventCursor = {

    def buildCursorWithPositionOn(feed:Feed[EntryType], index:Int) = {
      EntryPointer(
        feed.entries.head,
        feed.entries.tail,
        FeedPosition(feed.selfLink, index),
        feed
      )
    }

<<<<<<< HEAD
    def nextPageOrEnd(feed:Feed[EntryType]) = {
      // we go to next page of feed or we reached the EndOfEntries
=======
    def previousFeedOrEnd(feed:Feed[EntryType]) = {
      // we go to previous feed page or we reached the EndOfEntries
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
      def endOfEntries = {
        feedPosition match {
          case Some(feedPos) => EndOfEntries(feedPos)
          case None =>
            // rather exceptional situation, can only occurs if a feed is completely empty
            EndOfEntries(FeedPosition(feed.selfLink, 0))
        }
      }

<<<<<<< HEAD
      feed.nextLink match {
        case Some(nextLink) => EntryOnNextPage(nextLink)
=======
      feed.previousLink match {
        case Some(previousLink) => EntryOnPreviousFeed(previousLink)
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
        case None => endOfEntries
      }

    }

    if (feed.entries.nonEmpty) {
      feedPosition match {
        // no FeedPosition means start of new Feed
        case None => buildCursorWithPositionOn(feed, 0)
        case Some(feedPos) =>
          // are there still entries to process?
          val nextIndex = feedPos.index + 1
          val remainingEntries = feed.entries.drop(nextIndex)
          remainingEntries match {
<<<<<<< HEAD
            case Nil => nextPageOrEnd(feed)
=======
            case Nil => previousFeedOrEnd(feed)
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
            case _ =>
              val partialFeed = feed.copy(entries = remainingEntries)
              buildCursorWithPositionOn(partialFeed, nextIndex)
          }
      }
    } else {
<<<<<<< HEAD
      nextPageOrEnd(feed)
    }
  }

  private def cursorOnNextPage(link:Link) : Try[EventCursor] = {
=======
      previousFeedOrEnd(feed)
    }
  }

  private def cursorOnPreviousFeed(link:Link) : Try[EventCursor] = {
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
    feedProvider.fetchFeed(link.href.path).map { feed => buildCursor(feed) }
  }


  private def buildNextEntryCursor(entryPointer:EntryPointer) : Try[EventCursor] = {
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
<<<<<<< HEAD
          case Some(url) => EntryOnNextPage(url)
=======
          case Some(url) => EntryOnPreviousFeed(url)
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
        }
      }

    nextCursor match {
<<<<<<< HEAD
      // still a page to go? go fetch it
      case EntryOnNextPage(nextPageUrl) => cursorOnNextPage(nextPageUrl)
      // no next link? stop processing, all entries were consumed
=======
      // still a feed to go? go fetch it
      case EntryOnPreviousFeed(previousFeedUrl) => cursorOnPreviousFeed(previousFeedUrl)
      // no next feed link? stop processing, all entries were consumed
>>>>>>> AT-9: reverse first/last, next/previous link rel concepts
      case end @ EndOfEntries(_) => Success(end)
      // wrap nextCursor in Success
      case _ => Success(nextCursor)

    }
  }

}
