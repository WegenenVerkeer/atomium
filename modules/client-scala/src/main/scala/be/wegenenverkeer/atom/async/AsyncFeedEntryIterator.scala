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
        throw new IllegalStateException(s"next() called while iterator cursor of type $anyOther")
    }

  }

  /** Internal pointer to the current entry. */
  private trait Cursor {
    def nextCursor : Future[Cursor]
  }

  private object Cursor {

    /** Builds an [[Cursor]] for a [[Feed]].
      *
      * @return an [[EntryPointer]] pointing to the head of the [[Feed]] if non-empty,
      * otherwise returns an [[EndOfEntries]]
      */
    def fromHeadOfFeed(feed: Feed[EntryType]): Cursor = {

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

  private case class InitCursor(entryRef: Option[EntryRef] = None) extends Cursor {

    /** Builds the initial Cursor
      * This method will search from the start of the feed for the given entry
      */
    def nextCursor : Future[Cursor] = {
      feedProvider.fetchFeed().flatMap {
        feed => buildCursor(feed, entryRef)
      }
    }

    /** Build an EventCursor according to the following rules:
      *
      *  - If there is no `EntryRef` => create a new cursor starting from the head of the Feed.
      *  - If there is a valid `EntryRef` => drop the corresponding entry and create new cursor starting
      *    from the head of the Feed.
      *  - If the last element of this page is already consumed
      *    - If there is 'previous' Link  create an `EntryOnPreviousFeedPage` cursor for the next page of the feed
      *    - If there is no 'previous' Link then create an `EndOfEntries` cursor.
      */
    private def buildCursor(feed: Feed[EntryType], entryRefOpt: Option[EntryRef] = None): Future[Cursor] = {

      val transformedFeed =
        entryRefOpt match {
          case Some(ref) =>
            if(feed.entries.exists(_.id == ref.entryId)) {
              // drop entry corresponding to this EntryRef
              val remainingEntries = feed.entries.dropWhile(_.id != ref.entryId)
              remainingEntries match {
                case x :: xs => feed.copy(entries = xs)
                case Nil => feed.copy(entries = Nil)
              }
            } else {
              throw new EntryNotFoundException(ref)
            }

          case None => feed
        }

      if (transformedFeed.entries.nonEmpty) {
        // go for a new EntryPointer if it still have entries
        Future.successful(Cursor.fromHeadOfFeed(transformedFeed))
      } else {
        // it's the end of this Feed?
        // decide where to go: EndOfEntries or EntryOnPreviousFeedPage?
        feed.previousLink match {
          case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href)).nextCursor
          case None => Future.successful(EndOfEntries(entryRefOpt))
        }
      }

    }

  }
  private case class EntryPointer(currentEntry: Entry[EntryType],
                                  stillToProcessEntries: Entries,
                                  entryRef: EntryRef,
                                  feed: Feed[EntryType]) extends Cursor {

    /** The next [[Cursor]]
      *   - The next [[EntryPointer]] if it still have entries to be consumed.
      *   - An [[EndOfEntries]] in case current page is exhausted and no new page is available.
      */
    def nextCursor : Future[Cursor] = {

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





  private case class EntryOnPreviousFeedPage(previousFeedUrl: Url) extends Cursor {

    /** The next [[Cursor]].
      *
      * This method will load the previous page an return an [[EntryPointer]]
      * placed on the head element of this new page.
      */
    def nextCursor : Future[Cursor] = {
      feedProvider.fetchFeed(previousFeedUrl.path).map {
        feed => Cursor.fromHeadOfFeed(feed)
      }
    }
  }



  private case class EndOfEntries(lastEntryRef: Option[EntryRef]) extends Cursor {
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
