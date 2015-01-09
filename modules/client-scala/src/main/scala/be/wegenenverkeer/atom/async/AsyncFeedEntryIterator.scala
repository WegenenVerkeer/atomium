package be.wegenenverkeer.atom.async

import be.wegenenverkeer.atom._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, ExecutionContext}
import scala.language.higherKinds
import scala.util.control.NonFatal
import scala.util.{Failure, Try, Success}

class AsyncFeedEntryIterator[E] (feedProvider: AsyncFeedProvider[E], timeout:Duration, implicit val execCtx:ExecutionContext) {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  def hasNext: Future[Boolean] = {

    futureCursor.map {
      case _:EndOfEntries => false
      case _ => true
    } recover {
      case NonFatal(e) => false
    }
  }

  def next(): Option[Entry[E]] = {

    // future must be completed
    futureCursor.value match {

      case Some(Success(entryPointer:EntryPointer)) =>
        // start the next cursor
        futureCursor = entryPointer.nextCursor
        Some(entryPointer.currentEntry)

      case Some(Success(end:EndOfEntries)) => None

      case Some(Failure(e)) => throw e

      case None =>
        throw new IllegalStateException("next() called on a not-completed Async iterator ")

      // being exhaustive to make the compiler happy
      case Some(Success(anyOther)) =>
        throw new IllegalArgumentException(s"next() called while iterator cursor of type $anyOther")
    }

  }

  /** Internal pointer to the current entry. */
  private trait EntryCursor {
    def nextCursor : Future[EntryCursor]
  }

  private object EntryCursor {

    /** Builds an [[EntryCursor]] for a [[Feed]].
      *
      * @return an [[EntryPointer]] pointing to the head of the [[Feed]] is non-empty,
      * otherwise returns an [[EndOfEntries]]
      */
    def fromHeadOfFeed(feed: Feed[EntryType]): EntryCursor = {

      feed.entries match {
        case head :: tail =>
          val entryId = feed.entries.head.id
          EntryPointer(
            currentEntry = feed.entries.head,
            stillToProcessEntries = feed.entries.tail,
            entryRef = EntryRef(feed.resolveUrl(feed.selfLink.href), entryId),
            feed = feed
          )

        case Nil => EndOfEntries(None)
      }

    }
  }

  private case class InitCursor(feedEntryRef: Option[EntryRef] = None) extends EntryCursor {

    /** Builds the initial EntryCursor
      * This method will search from the start of the feed for the given entry
      */
    def nextCursor : Future[EntryCursor] = {
      feedProvider.fetchFeed().map {
        feed => buildCursor(feed, feedEntryRef)
      }
    }

    /** Build an EventCursor according to the following rules:
      *
      *  - If there is no FeedEntryRef => create a new cursor starting from the head of the Feed.
      *  - If there is a valid FeedEntryRef => drop the corresponding entry and create new cursor starting
      *  from the head of the Feed.
      *  - If the last element of this page is already consumed
      *    - If there is 'previous' Link  create an EntryOnPreviousFeedPage cursor for the next page of the feed
      *    - If there is no 'previous' Link then create an EndOfEntries cursor.
      */
    private def buildCursor(feed: Feed[EntryType], entryRefOpt: Option[EntryRef] = None): EntryCursor = {

      val transformedFeed =
        entryRefOpt match {
          case Some(ref) =>
            // drop entry corresponding to this FeedEntryRef
            val remainingEntries = feed.entries.filter(_.id != ref.entryId)
            feed.copy(entries = remainingEntries)

          case None => feed
        }

      if (transformedFeed.entries.nonEmpty) {
        // go for a new EntryPointer if it still have entries
        EntryCursor.fromHeadOfFeed(transformedFeed)
      } else {
        // it's the end of this Feed?
        // decide where to go: EndOfEntries or EntryOnPreviousFeedPage?
        feed.previousLink match {
          case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href))
          case None => EndOfEntries(entryRefOpt)
        }
      }

    }

  }
  private case class EntryPointer(currentEntry: Entry[EntryType],
                                  stillToProcessEntries: Entries,
                                  entryRef: EntryRef,
                                  feed: Feed[EntryType]) extends EntryCursor {

    /** The next [[EntryCursor]]
      *   - The next [[EntryPointer]] on current page if still entries to be consumed.
      *   - An [[EntryOnPreviousFeedPage]] in case current page is exhausted and a new page is available.
      *   - An [[EndOfEntries]] in case current page is exhausted and no new page is available.
      */
    def nextCursor : Future[EntryCursor] = {

      if (stillToProcessEntries.nonEmpty) {
        val nextEntryId = stillToProcessEntries.head.id
        Future.successful {
          copy(
            currentEntry = stillToProcessEntries.head,
            stillToProcessEntries = stillToProcessEntries.tail, // moving forward, dropping head
            entryRef = entryRef.copy(entryId = nextEntryId) // moving position forward
          )
        }
      } else {
        feed.previousLink match {
          case None => Future.successful(EndOfEntries(Some(entryRef)))
          case Some(link) => EntryOnPreviousFeedPage(feed.resolveUrl(link.href)).nextCursor
        }
      }
    }
  }





  private case class EntryOnPreviousFeedPage(previousFeedUrl: Url) extends EntryCursor {

    /** The next [[EntryCursor]].
      *
      * This method will load the previous page an return an [[EntryCursor]].
      */
    def nextCursor : Future[EntryCursor] = {
      feedProvider.fetchFeed(previousFeedUrl.path).map {
        feed => EntryCursor.fromHeadOfFeed(feed)
      }
    }
  }



  private case class EndOfEntries(lastEntryRef: Option[EntryRef]) extends EntryCursor {
    /** Throws a NonSuchElementException since there is no nextCursor for a [[EndOfEntries]] */
    def nextCursor : Future[EntryCursor] = throw new NoSuchElementException("No new entries available!")
  }

  private var futureCursor: Future[EntryCursor] = InitCursor(feedProvider.initialEntryRef).nextCursor



}

object AsyncFeedEntryIterator {
  object Implicits {
    implicit class AsyncIteratorBuilder[T](feedProvider: AsyncFeedProvider[T]) {
      def iterator(timeout:Duration)(implicit ec: ExecutionContext) = new AsyncFeedEntryIterator(feedProvider, timeout, ec)
    }
  }
}
