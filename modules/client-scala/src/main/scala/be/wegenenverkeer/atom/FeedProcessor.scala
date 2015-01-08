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


  /**
   * Start the consuming of Feeds. Returns a {{{Try[Unit]}}}.
   * In case of success a Success[Unit]
   * or in case of failure a Failure[FeedProcessingException]
   *
   * TODO: we should find a better way for doing this. Eventually two method, start() and startManaged().
   * @return Try[Unit].
   *
   */
  def start(): AtomResult[E] = {

    // Bloody hack: Scala-ARM needs a Manifest for the managed resource,
    // but we don't want to pollute the interface if it, because we intend to use it from Java as well
    implicit val manif = new Manifest[FeedPageProvider[E]] {
      override def runtimeClass: Class[_] = classOf[FeedPageProvider[E]]
    }

    implicit val feedProvResource = FeedPageProvider.managedFeedProvider(feedProvider)


    // FeedProvider must be managed
    val extResult = managed(feedProvider).map { provider =>

        val feedIterator = new FeedEntryIterator(feedProvider)

        @tailrec
        def consume(atomResult:AtomResult[E]) : AtomResult[E] = {

          def processNextEntry: AtomResult[E] = {

            // pick next entry
            val nextEntry = Try(feedIterator.next())

            // consume it
            val result = nextEntry.flatMap {
              // consume entry if exist
              case Some(entry) =>
                // NOTE: entryConsumer returns a Try[Entry[E]]
                // but we need Try[Option[Entry[E]]. Hence the extra mapping.
                entryConsumer(entry).map(Option(_))

              // no entry produced? we just return a Success with a None inside
              case None => Success(None)
            }

            // transform the final result to a AtomResult
            result match {
              // entity effectively consumed
              case Success(Some(consumedEntry)) => AtomSuccess(consumedEntry)

              // no entity consumed, we keep the last successful entry we know
              case Success(None) => AtomSuccess(atomResult.lastSuccessfulEntry)

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

    extResult.either match {
      case Right(x) => x
      case Left(throwables) =>
        val messages = throwables.map(_.getMessage).mkString("[", "] | [", "[")
        throw FeedProcessingException(None, messages)
    }
  }


}
