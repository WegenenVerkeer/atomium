package be.wegenenverkeer.atomium.client

import java.time.LocalDateTime

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import be.wegenenverkeer.atomium.client.ImplicitConversions._
import be.wegenenverkeer.atomium.japi
import be.wegenenverkeer.atomium.japi.client.FeedEntry
import be.wegenenverkeer.atomium.client.AbstractAtomiumFeedClientActor.Protocol.HasPosition
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.json._
import rx.lang.scala.{Observable, Subscriber}

import scala.collection.immutable
import scala.util.Try

/**
 * Generic Actor that consumes Atomium feeds (in json format) via the AtomiumClient.
 * If the feed handling goes wrong, the actor will stop (whatever the failure type is).
 * It is therefore suggested to run this actor within a BackoffSupervisor or similar construct.
 *
 * This actor only sends messages to itself. The feed entries are rapported by the [[publishEvent]] method.
 *
 * It's the job of the subclass to decide what to do with these entries.
 *
 * @param config feed and actor config
 * @param eventFormat format for EventBus events
 * @param feedEntryFormat Reads for reading entries in the feed
 * @tparam EVENT type of the event that will be put on the eventbus
 * @tparam T (domain) type to deserialize the json feed entries to
 */
abstract class AbstractAtomiumFeedClientActor[EVENT <: HasPosition, T](config: FeedConfig,
                                                                              eventFormat: Format[EVENT],
                                                                              feedEntryFormat: Reads[T],
                                                                              monitorActor: ActorRef
                                                                               ) extends PersistentActor with ActorLogging {

  import AbstractAtomiumFeedClientActor.Protocol._

  lazy val atomiumClient = new japi.client.AtomiumClient.Builder()
    .setBaseUrl(config.baseUrl)
    .setAcceptJson()
    .setConnectTimeout(config.connectTimeout.toMillis.toInt)
    .build
    .asScala

  var positionInFeed: Option[FeedPosition] = None

  def receiveRecover: Receive = {
    case RecoveryCompleted =>
      log.debug("{}: Recovery completed, start the feed observer", config.feedUrl)
      subscribeToFeed()

    case jsValue: JsValue => onJsValueEvent(jsValue)

    case x: SnapshotOffer => positionInFeed = x.snapshot.asInstanceOf[Option[FeedPosition]]
  }

  /**
   * Abstract method to be implemented by concrete AtomiumFeedClients.
   * Should convert a NewFeedEntryReceived message (containing the feed entry and position)
   * to an event of type EVENT. The event will than be published via the publishEvent method,
   * after it has been persisted.
   *
   * @param entryReceivedMessage a message that contains a new feed entry and position
   * @return an event of type EVENT
   */
  def createEventToPublish(entryReceivedMessage: NewFeedEntryReceived[T]): EVENT

  /**
   * New entries in the feed are transformed into events. This method will be called with
   * the event as parameter when new entries are received.
   *
   * @param event The event that is based on an new entry in the feed.
   */
  def publishEvent(event: EVENT): Unit

  /**
   * Implement this method to get informed about the status of the feed client.
   * The default implementation just ignores the info.
   *
   * @param atomiumFeedClientStatus the current status
   */
  def status(atomiumFeedClientStatus: AtomiumFeedClientStatus): Unit = ()

  override def receiveCommand: Receive = {

    case c: NewFeedEntryReceived[T] =>
      val event = createEventToPublish(c)
      persistEvent(event) { evt =>
        updateState(evt)
        log.debug("{}: entry on position {} saved succesfully, putting on eventbus", config.feedUrl, evt.position)
        publishEvent(event)

        status(AtomiumFeedClientStatus(
          ok = true,
          status = "receiving",
          feedPosition = Some(evt.position.selfHref),
          entryId = Some(evt.position.entryId),
          receivedOn = Some(evt.position.receivedOn)))
      }

    case e: FeedReadError =>
      // the atomium client observer is now unsubscribed, so we will receive no more events in this subscription
      log.error(e.error, "{}: Error while reading feed, last known position was {}, will stop now", config.feedUrl, positionInFeed)

      // close should not cause errors here
      Try(atomiumClient.close())

      status(AtomiumFeedClientStatus(
        ok = false,
        status = "error",
        error = Some(e.error.getMessage),
        feedPosition = positionInFeed.map(_.selfHref),
        entryId = positionInFeed.map(_.entryId),
        receivedOn = positionInFeed.map(_.receivedOn)
      ))

      context.stop(self)

    case GetStatusInfo =>
      sender() ! positionInFeed
  }

  private def updateState(event: EVENT) = {
    positionInFeed = Some(event.position)
  }


  protected def subscribeToFeed(): Unit = {

    def createObservable: Observable[FeedEntry[JsonNode]] = {
      val feedObservableBuilder = atomiumClient.feed(config.feedUrl, classOf[JsonNode])

      val observable = positionInFeed match {
        case Some(position) =>
          log.info("{}: Start observing from position {}", config.feedUrl, position)

          status(AtomiumFeedClientStatus(
            ok = true,
            status = "startObversing",
            feedPosition = positionInFeed.map(_.selfHref),
            entryId = positionInFeed.map(_.entryId),
            receivedOn = positionInFeed.map(_.receivedOn)
          ))

          feedObservableBuilder.inner.observeFrom(position.entryId, position.selfHref, config.pollingInterval.toMillis.toInt)


        case None =>
          log.info("{}: Start observing from the start", config.feedUrl)

          status(AtomiumFeedClientStatus(
            ok = true,
            status = "startObversing",
            feedPosition = Some("beginOfFeed")
          ))

          feedObservableBuilder.inner.observeFromBeginning(config.pollingInterval.toMillis.toInt)

      }
      rx.lang.scala.JavaConversions.toScalaObservable(observable)
    }

    createObservable.subscribe(new Subscriber[FeedEntry[JsonNode]]() {

      override def onNext(entry: FeedEntry[JsonNode]): Unit = {
        val position = FeedPosition(
          selfHref = entry.getSelfHref,
          entryId = entry.getEntry.getId,
          receivedOn = LocalDateTime.now()
        )
        log.debug("{}: FeedEntry received from position {}", config.feedUrl, position)

        // since atomium client cannot configure it's own serializer yet, we need to convert the jackson json to play-json
        // (works reasonable efficient), en then use our own play formatter on it
        val value = Json.toJson(entry.getEntry.getContent.getValue)
        val feedEntry = Json.fromJson[T](value)(feedEntryFormat).get

        // warning: at this point, we will put as many entries on our mailbox as atomiumclient has in it's feed
        // if the processing cannot keep up, we will get a very big mailbox
        // we could possibly unsubscribe on the feed until our mailbox has been partially processed
        // or even better would be if we could use backpressure on the feed client
        self ! NewFeedEntryReceived(
          entry = feedEntry,
          position = position
        )
      }

      override def onError(error: Throwable): Unit = {
        self ! FeedReadError(error)
      }

    })
  }


  def onJsValueEvent(jsValue: JsValue): Unit = {

    eventFormat.reads(jsValue) match {
      case JsSuccess(evt, _) =>
        updateState(evt)

      case JsError(errors) =>
        val msg = s"Can't read event: $jsValue - ${JsError.toJson(errors)}"
        log.error(msg)
        throw new RuntimeException(msg)
    }
  }

  def persistEvent[E <: EVENT](event: E)(handler: E => Unit): Unit = {

    val jsValue = eventFormat.writes(event)
    persist(jsValue) { _ =>
      handler(event)
    }
  }

  def persistEvents[E <: EVENT](events: immutable.Seq[E])(handler: E => Unit): Unit = {
    events.foreach(e => persistEvent(e)(handler))
  }

}


object AbstractAtomiumFeedClientActor {

  object Protocol {

    case class NewFeedEntryReceived[T](entry: T, position: FeedPosition)

    case class FeedReadError(error: Throwable)

    /**
     * Ask for the optional last feed Position
     */
    object GetStatusInfo


    // datatypes
    case class FeedPosition(selfHref: String, entryId: String, receivedOn: LocalDateTime = LocalDateTime.now())

    object FeedPosition {

      implicit val positionFormat = Json.format[FeedPosition]
    }

    trait HasPosition {

      def position: FeedPosition

    }


  }

}