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

  case class CurrentEvent(eventTeVerwerken: Entry[EntryType],
                          nogTeVerwerkenEvents: Entries,
                          feedPosition:FeedPosition,
                          feed:Feed[EntryType]) extends EventCursor

  case class EventOnNextFeed(nextFeedUrl:String) extends EventCursor
  case object EndOfEvents extends EventCursor


  /**
   * Ofwel zijn de Feeds geconsumeerd en we hebben niks om terug te geven ofwel
   * hebben we een foutmelding.
   *
   * Dus, het Success geval is een Unit. Data is geconsumeerd en
   * er is niks om terug te geven.
   */
  def start() : ValidationNel[String, Unit] = {
    initEventCursor() match {
      case Success(eventCursor) => process(eventCursor)
      case Failure(err) => err.failure[Unit]
    }
  }


  private def initEventCursor() : ValidationNel[String, EventCursor] = {

    val feedResult = initialPosition.fold {
      // geen positie, fetch eerste feed
      feedProvider.fetchFeed()
    } { feedPos =>
      // fetch feed voor processing
      feedProvider.fetchFeed(feedPos.page)
                     }

    feedResult.map { feed => buildCursor(feed, initialPosition) }
  }


  @tailrec
  private def process(cursor:EventCursor) : ValidationNel[String, Unit] = {

    //!!! NOTE !!!
    // code hieronder bevat twee pattern matching over een ValidationNel[String, EventCursor]
    // die precies hetzelfde zijn.
    // Moet zo blijven anders kan scalac geen tail call opt doen
    // en we WILLEN een tailrec hier
    cursor match {
      case currentEvent:CurrentEvent =>

        val nextEventOrFailure = for {
          _ <- consumeEvent(currentEvent)
          nextEvent <- buildVolgendeEventCursor(currentEvent)
        } yield nextEvent

        // gelukt? process volgende event
        // volgende EventCursor kan een EndOfEvents of een EventOnNextFeed
        // in geval van een EventOnNextFeed, we kunnen we een fout krijgen bij ophalen van volgende feed
        nextEventOrFailure match {
          case Success(next) => process(next)
          case Failure(errorMessages) => errorMessages.failure[Unit]
        }

      case EventOnNextFeed(nextFeedUrl) =>
        cursorOnNextFeed(nextFeedUrl) match {
          case Success(next) => process(next)
          case Failure(errorMessages) => errorMessages.failure[Unit]
        }

      case EndOfEvents => ().successNel[String] // we zijn klaar
    }

  }

  private def consumeEvent(currentEvent:CurrentEvent) : ValidationNel[String, Unit] = {
    try {
      entryConsumer.consume(currentEvent.feedPosition, currentEvent.eventTeVerwerken)
    } catch {
      case ex:Exception => ex.getMessage.failureNel[Unit]
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
    def builCursorMetPositieOp(feed:Feed[EntryType], index:Int) = {
      CurrentEvent(
        feed.entries.head,
        feed.entries.tail,
        FeedPosition(feed.selfLink, index),
        feed
      )
    }

    def volgendeFeedOfEinde(feed:Feed[EntryType]) = {
      // ofwel gaan we naar een volgende feed, ofwel is het gedaan
      feed.nextLink.map(EventOnNextFeed).getOrElse(EndOfEvents)
    }

    if (feed.entries.nonEmpty) {
      feedPosition match {
        // geen FeedPosition betekent begin van een nieuwe Feed
        case None => builCursorMetPositieOp(feed, 0)
        case Some(feedPos) =>
          // zijn er nog entries om te verwerken
          val volgendeIndex = feedPos.index + 1
          val remainingEntries = feed.entries.drop(volgendeIndex)
          remainingEntries match {
            case Nil => volgendeFeedOfEinde(feed)
            case _ =>
              val partialFeed = feed.copy(entries = remainingEntries)
              builCursorMetPositieOp(partialFeed, volgendeIndex)
          }
      }
    } else {
      volgendeFeedOfEinde(feed)
    }
  }

  private def cursorOnNextFeed(url:String) : ValidationNel[String, EventCursor] = {
    feedProvider.fetchFeed(url).map { feed => buildCursor(feed) }
  }


  private def buildVolgendeEventCursor(eventCursor:CurrentEvent) : ValidationNel[String, EventCursor] = {
    val volgendeCursor =
      if (eventCursor.nogTeVerwerkenEvents.nonEmpty) {
        eventCursor.copy(
          eventTeVerwerken = eventCursor.nogTeVerwerkenEvents.head,
          nogTeVerwerkenEvents = eventCursor.nogTeVerwerkenEvents.tail, // moving forward, dropping head
          feedPosition = eventCursor.feedPosition.copy(index = eventCursor.feedPosition.index + 1) // moving position forward
        )
      } else {
        eventCursor.feed.nextLink match {
          case None => EndOfEvents
          case Some(url) => EventOnNextFeed(url)
        }
      }

    volgendeCursor match {
      // nog een volgende feed, go fetch it
      case EventOnNextFeed(nextFeedUrl) => cursorOnNextFeed(nextFeedUrl)
      // geen next feed link? stop processing, alle events zijn dus verwerkt
      case EndOfEvents => EndOfEvents.successNel[String]
      // wrap volgendeCursor in Validation voor volgende stap
      case _ => volgendeCursor.successNel[String]

    }
  }


}
