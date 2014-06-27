package be.vlaanderen.awv.atom

import scala.annotation.tailrec
import scalaz._
import Scalaz._

class FeedProcessor[E](initialPosition:Option[FeedPosition],
                       feedProvider: FeedProvider[E],
                       entryConsumer: EntryConsumer[E]) {

  def this(initialPosition:FeedPosition, feedProvider: FeedProvider[E], entryConsumer: EntryConsumer[E]) =
    this(Option(initialPosition), feedProvider, entryConsumer)
  
  type EntryType = E
  type Entries = List[Entry[EntryType]]

  trait EventCursor

  case class EntryPointer(entryToProcess: Entry[EntryType],
                          stillToProcessEntries: Entries,
                          feedPosition:FeedPosition,
                          feed:Feed[EntryType]) extends EventCursor

  case class EntryOnNextFeed(nextFeedUrl:Link) extends EventCursor
  case class EndOfEntries(lastFeedPosition:FeedPosition) extends EventCursor


  /**
   * Ofwel zijn de Feeds geconsumeerd en we hebben niks om terug te geven ofwel
   * hebben we een foutmelding.
   *
   * Dus, het Success geval is een Unit. Data is geconsumeerd en
   * er is niks om terug te geven.
   */
  def start() : Validation[FeedProcessingError, FeedPosition] = {
    initEventCursor match {
      case Success(eventCursor) => process(eventCursor)
      case Failure(err) => err.failure[FeedPosition]
    }
  }


  private def initEventCursor : Validation[FeedProcessingError, EventCursor] = {

    val feedResult = initialPosition.fold {
      // no initial position, fetch first feed
      feedProvider.fetchFeed()
    } { feedPos =>
      // fetch feed according to position
      feedProvider.fetchFeed(feedPos.link.href.path)
    }

    feedResult.map { feed => buildCursor(feed, initialPosition) }
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
        nextEventOrFailure match {
          case Success(next) => process(next)
          case Failure(errorMessages) => errorMessages.failure[FeedPosition]
        }

      case EntryOnNextFeed(nextFeedUrl) =>
        cursorOnNextFeed(nextFeedUrl) match {
          case Success(next) => process(next)
          case Failure(errorMessages) => errorMessages.failure[FeedPosition]
        }

      case EndOfEntries(lastFeedPos) => lastFeedPos.success[FeedProcessingError] // we are done
    }

  }

  private def consumeEvent(currentEvent:EntryPointer) : FeedProcessingResult = {
    try {
      entryConsumer.consume(currentEvent.feedPosition, currentEvent.entryToProcess)
    } catch {
      case ex:Exception =>
        FeedProcessingError(
          currentEvent.feedPosition,
          ex.getMessage
        ).failure[FeedPosition]
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
   *      <li>Ga naar de volgende feed als een Link 'next' bestaat. Maak een EventOnNextFeed cursor.</li>
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

    def nextFeedOrEnd(feed:Feed[EntryType]) = {
      // we go to next feed page or we reached the EndOfEntries
      def endOfEntries = {
        feedPosition match {
          case Some(feedPos) => EndOfEntries(feedPos)
          case None =>
            // rather exceptional situation, can only occurs if a feed is completely empty
            EndOfEntries(FeedPosition(feed.selfLink, 0))
        }
      }

      feed.nextLink match {
        case Some(nextLink) => EntryOnNextFeed(nextLink)
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
            case Nil => nextFeedOrEnd(feed)
            case _ =>
              val partialFeed = feed.copy(entries = remainingEntries)
              buildCursorWithPositionOn(partialFeed, nextIndex)
          }
      }
    } else {
      nextFeedOrEnd(feed)
    }
  }

  private def cursorOnNextFeed(link:Link) : Validation[FeedProcessingError, EventCursor] = {
    feedProvider.fetchFeed(link.href.path).map { feed => buildCursor(feed) }
  }


  private def buildNextEntryCursor(entryPointer:EntryPointer) : Validation[FeedProcessingError, EventCursor] = {
    val nextCursor =
      if (entryPointer.stillToProcessEntries.nonEmpty) {
        entryPointer.copy(
          entryToProcess = entryPointer.stillToProcessEntries.head,
          stillToProcessEntries = entryPointer.stillToProcessEntries.tail, // moving forward, dropping head
          feedPosition = entryPointer.feedPosition.copy(index = entryPointer.feedPosition.index + 1) // moving position forward
        )
      } else {
        entryPointer.feed.nextLink match {
          case None => EndOfEntries(entryPointer.feedPosition)
          case Some(url) => EntryOnNextFeed(url)
        }
      }

    nextCursor match {
      // still a feed to go? go fetch it
      case EntryOnNextFeed(nextFeedUrl) => cursorOnNextFeed(nextFeedUrl)
      // no next feed link? stop processing, all entries were consumed
      case end @ EndOfEntries(_) => end.success[FeedProcessingError]
      // wrap nextCursor in Validation
      case _ => nextCursor.success[FeedProcessingError]

    }
  }


}
