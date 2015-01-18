package be.wegenenverkeer.atom.async

import be.wegenenverkeer.atom._

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Failure, Success}

class AsyncFeedEntryIterator[E] (feedProvider: AsyncFeedProvider[E], timeout:Duration)(implicit val execCtx:ExecutionContext) {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  private var futureCursor: Future[Cursor] = InitCursor(feedProvider.initialEntryRef).nextCursor

  def hasNext: Future[Boolean] = {

    futureCursor.map {
      case _:EndOfEntries => false
      case _ => true
    }
  }

  def next(): EntryRef[E] = {

    // future must be completed
    futureCursor.value match {

      case Some(Success(entryPointer:EntryPointer)) =>
        // start the next cursor
        futureCursor = entryPointer.nextCursor
        entryPointer.current

      case Some(Success(end:EndOfEntries)) =>
        throw new NoSuchElementException("next() called on a terminated iterator ")

      case Some(Failure(e)) => throw e

      case None =>
        throw new IllegalStateException("next() called on a not-completed async iterator")

      // being exhaustive to make the compiler happy
      case Some(Success(anyOther)) =>
        throw new IllegalStateException(s"next() called while iterator cursor of type $anyOther")
    }

  }

  /** Internal pointer to the current entry. */
  private trait Cursor {
    def nextCursor : Future[Cursor]
  }

  private object LastOfFeed {


    /** Builds an [[Cursor]] for a [[Feed]].
      *
      * @return an [[EntryPointer]] pointing to the last entry of the [[Feed]] if non-empty,
      * otherwise returns an [[EndOfEntries]]
      */
    def apply(feed: Feed[EntryType], lastEntryRef:Option[EntryRef[EntryType]] = None): Cursor = {
      if (feed.entries.nonEmpty) {
        EntryPointer(feed.entries, feed)
      } else {
        EndOfEntries(lastEntryRef)
      }
    }
  }

  private case class InitCursor(entryRefOpt: Option[EntryRef[EntryType]] = None) extends Cursor {

    /** Build an EventCursor according to the following rules:
      *
      *  - If there is no `EntryRef` => create a new cursor starting from the head of the Feed.
      *  - If there is a valid `EntryRef` => drop the corresponding entry and create new cursor starting
      *    from the head of the Feed.
      *  - If the last element of this page is already consumed
      *    - If there is 'previous' Link  create an `EntryOnPreviousFeedPage` cursor for the next page of the feed
      *    - If there is no 'previous' Link then create an `EndOfEntries` cursor.
      */
    def nextCursor : Future[Cursor] = {

      entryRefOpt match {
        // if there is a EntryRef, we fetch the page where it is
        // expected to be and we build a cursor with it
        case Some(entryRef) =>
          feedProvider.fetchFeed(entryRef.url.path).flatMap { feed =>
            buildCursor(feed, entryRef)
          }

        // no initial EntryRef? We fetch the page from the beginning
        // and build a cursor pointing to last entry of the Feed
        case None =>
          feedProvider.fetchFeed().map { feed =>
            LastOfFeed(feed)
          }
      }

    }


    private def buildCursor(feed: Feed[EntryType], entryRef: EntryRef[EntryType]): Future[Cursor] = {

      val entryOpt = feed.entries.find { entry =>
        entry.id == entryRef.entryId
      }

      if (entryOpt.isEmpty) {
        Future.failed(new NoSuchElementException(s"$entryRef not available on feed - ${feed.selfLink}"))
      } else {

        val entriesToProcess = {
          // drop everything up to and including the Entry referenced by `entryRef`
          feed.entries.takeWhile { entry =>
            entry.id != entryRef.entryId
          }
        }

        if (entriesToProcess.nonEmpty) {
          // go for a new EntryPointer if it still have entries
          Future.successful(EntryPointer(entriesToProcess, feed))
        } else {
          // Is its the end of this Feed?
          // decide where to go: EndOfEntries or EntryOnPreviousFeedPage?
          feed.previousLink match {
            case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href), entryRef).nextCursor
            case None => Future.successful(EndOfEntries(Option(entryRef)))
          }
        }
      }
    }
  }


  private case class EntryPointer(entries: Entries,
                                  feed: Feed[EntryType]) extends Cursor {

    val entriesReversed = entries.reverse

    val current = EntryRef(feed, entriesReversed.head)
    val stillToProcessEntries = entriesReversed.tail.reverse

    /** The next [[Cursor]]
      *   - The next [[EntryPointer]] if it still have entries to be consumed.
      *   - An [[EndOfEntries]] in case current page is exhausted and no new page is available.
      */
    def nextCursor : Future[Cursor] = {

      if (stillToProcessEntries.nonEmpty) {
        Future.successful(copy(entries = stillToProcessEntries))
      } else {
        feed.previousLink match {
          case Some(link) => EntryOnPreviousFeedPage(feed.resolveUrl(link.href), current).nextCursor
          case None => Future.successful(EndOfEntries(Some(current)))
        }
      }
    }
  }

  private object EntryPointer {
    def apply(feed:Feed[EntryType]) : EntryPointer = EntryPointer(feed.entries, feed)
  }





  private case class EntryOnPreviousFeedPage(previousFeedUrl: Url, lastEntryRef:EntryRef[EntryType]) extends Cursor {

    /** The next [[Cursor]].
      *
      * This method will load the previous page an return an [[EntryPointer]]
      * placed on the head element of this new page.
      */
    def nextCursor : Future[Cursor] = {
      feedProvider.fetchFeed(previousFeedUrl.path).map {
        feed => LastOfFeed(feed, Some(lastEntryRef))
      }
    }
  }



  private case class EndOfEntries(lastEntryRef: Option[EntryRef[EntryType]]) extends Cursor {
    /** Throws a NonSuchElementException since there is no nextCursor for a [[EndOfEntries]] */
    def nextCursor : Future[Cursor] = throw new NoSuchElementException("No new entries available!")
  }





}

object AsyncFeedEntryIterator {
  object Implicits {
    implicit class AsyncIteratorBuilder[T](feedProvider: AsyncFeedProvider[T]) {
      def iterator(timeout:Duration)(implicit ec: ExecutionContext) = new AsyncFeedEntryIterator(feedProvider, timeout)
    }
  }
}
