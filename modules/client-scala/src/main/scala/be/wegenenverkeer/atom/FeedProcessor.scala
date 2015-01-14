package be.wegenenverkeer.atom

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/** A feed processor fetches pages from the feed and offers the new items to the entry consumer.
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

  def start(): AtomResult[E] = {
    import scala.concurrent.duration._
    implicit val timeout = 5 seconds
    import scala.concurrent.ExecutionContext.Implicits.global
    start(timeout)
  }

  /** Start the consuming of Feeds.
    *
    * @return In case of success a [[AtomSuccess]] containing the last successful consumed entry
    *         or a [[AtomNothing]] if nothing is processed.
    *         In case of failure a [[AtomFailure]] containing the thrown exception and the last successful
    *         consumed entry if any.
    */
  def start(timeout:Duration)(implicit execContext:ExecutionContext): AtomResult[E] = {

    val feedIterator = new FeedEntryIterator(feedProvider, timeout)

    @tailrec
    def consume(atomResult:AtomResult[E]) : AtomResult[E] = {

      def processNextEntry: AtomResult[E] = {

        // pick next entry
        val nextEntry = Try(feedIterator.next())

        // consume it
        val result = nextEntry.flatMap { entryRef =>
            // consume entryRef if exist
            entryConsumer(entryRef.entry)
        }

        // transform the final result to a AtomResult
        result match {
          // entity effectively consumed
          case Success(consumedEntry) => AtomSuccess(consumedEntry)

          // on failure, we keep the last successful entry
          // can be empty if it fails when consuming first entry
          case Failure(ex) => AtomFailure(atomResult.lastSuccessfulEntry, ex)
        }
      }


      atomResult match {
        // fail-fast on failure
        // can fail when fetching an entry or when consuming it
        // in any case we stop processing
        case failure: AtomFailure[_] => failure

        // keep consuming if it still have entries
        case any if feedIterator.hasNext => consume(processNextEntry)

        // no entries? return whatever result we have
        case any if !feedIterator.hasNext => atomResult

      }
    }

    consume(AtomNothing)
  }

}
