package be.vlaanderen.awv.atom

import scala.annotation.tailrec
import resource._

import scala.util.{Failure, Success, Try}

class FeedProcessor[E](feedProvider: FeedProvider[E],
                       entryConsumer: EntryConsumer[E]) {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  trait EventCursor
  case class EntryPointer(entryToProcess: Entry[EntryType],
                          stillToProcessEntries: Entries,
                          feedPosition:FeedPosition,
                          feed:Feed[EntryType]) extends EventCursor

  case class EntryOnPreviousFeed(previousFeedUrl:Link) extends EventCursor
  case class EndOfEntries(lastFeedPosition:FeedPosition) extends EventCursor


  /**
   * Start the consuming of Feeds. Returns a {{{Try[Unit]}}}.
   * Failure a {{{FeedProcessingError}}}.
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
    // code hieronder bevat twee pattern matching over een Validation[String, EventCursor]
    // die precies hetzelfde zijn.
    // Moet zo blijven anders kan scalac geen tail call opt doen
    // en we WILLEN een tailrec hier
    cursor match {
      case currentEvent:EntryPointer =>

        val nextEventOrFailure = for {
          _ <- consumeEvent(currentEvent)
          nextEvent <- buildNextEntryCursor(currentEvent)
        } yield nextEvent

        // gelukt? process volgende event
        // volgende EventCursor kan een EndOfEvents of een EventOnNextFeed
        // in geval van een EventOnNextFeed, we kunnen we een fout krijgen bij ophalen van volgende feed
        // map on Try does not work here, because of tailrec
        nextEventOrFailure match {
          case Success(next) => process(next)
          case Failure(ex) => Failure(ex)
        }

      // map on Try does not work here, because of tailrec
      case EntryOnPreviousFeed(previousFeedUrl) =>
        cursorOnPreviousFeed(previousFeedUrl) match {
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
   * Build een EventCursor volgens de volgende regels:
   *
   * <ul>
   *  <li>Als er geen FeedPosition is, maak een nieuwe cursor met een volledige Feed en index op 0</li>
   *  <li>Is er een geldig FeedPosition, increase index, drop entries tot aan de nieuwe index en maak nieuwe cursor.</li>
   *  <li>Is het laatste element van die Feed al geconsumeerd?
   *    <ul>
   *      <li>Ga naar de vorige feed (more recent) als een Link 'previous' bestaat. Maak een EventOnPreviousFeed cursor.</li>
   *      <li>Is er geen Link naar een volgende Feed? Dan maak een EndOfEvents cursor.</li>
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

    def previousFeedOrEnd(feed:Feed[EntryType]) = {
      // we go to previous feed page or we reached the EndOfEntries
      def endOfEntries = {
        feedPosition match {
          case Some(feedPos) => EndOfEntries(feedPos)
          case None =>
            // rather exceptional situation, can only occurs if a feed is completely empty
            EndOfEntries(FeedPosition(feed.selfLink, 0))
        }
      }

      feed.previousLink match {
        case Some(previousLink) => EntryOnPreviousFeed(previousLink)
        case None => endOfEntries
      }

    }

    if (feed.entries.nonEmpty) {
      feedPosition match {
        // geen FeedPosition betekent begin van een nieuwe Feed
        case None => buildCursorWithPositionOn(feed, 0)
        case Some(feedPos) =>
          // zijn er nog entries om te verwerken
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

  private def cursorOnPreviousFeed(link:Link) : Try[EventCursor] = {
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
          case Some(url) => EntryOnPreviousFeed(url)
        }
      }

    nextCursor match {
      // still a feed to go? go fetch it
      case EntryOnPreviousFeed(previousFeedUrl) => cursorOnPreviousFeed(previousFeedUrl)
      // no next feed link? stop processing, all entries were consumed
      case end @ EndOfEntries(_) => Success(end)
      // wrap nextCursor in Success
      case _ => Success(nextCursor)

    }
  }

}
