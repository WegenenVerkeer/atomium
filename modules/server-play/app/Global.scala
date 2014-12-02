import akka.japi.Option.Some
import be.vlaanderen.awv.atom._
import controllers.{Event, EventController, MemoryFeedStore, StringController}
import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter
import _root_.java.util.{TimerTask, Timer}

import scala.util.Random

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

  //string service

  val id = "my_feed"
  val stringStore: AbstractFeedStore[String] = new MemoryFeedStore[String](id, Url("http://localhost:9000/feeds/"), Some("strings of life"))
  val stringService = new FeedService[String, Context](id, 2, { (s, c) => stringStore })
  val stringController = new StringController(stringService)

  //add some dummy values to the stringService
  implicit val c: Context = new Context() {}
  stringService.push("foo")
  stringService.push(List("bar", "baz"))
  stringService.push("foobar")

  //event service

  val events_id = "events"
  val eventStore: AbstractFeedStore[Event] = new MemoryFeedStore[Event](events_id, Url("http://localhost:9000/feeds/"), Some("events"), "application/xml")
  val eventService = new FeedService[Event, Context](events_id, 10, { (s, c) => eventStore })
  val eventController = new EventController(eventService)

  1 to 25 foreach {
    i => eventService.push(Event(Random.nextDouble(), Some(s"populated at start $i")))
  }

  val timer = new Timer()
  timer.scheduleAtFixedRate(new TimerTask {
    override def run() = { eventService.push(Event(Random.nextDouble(), None)) }
  }, 1000L, 1000L)


  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    if (controllerClass == classOf[StringController]) {
      stringController.asInstanceOf[A]
    } else if (controllerClass == classOf[EventController]) {
      eventController.asInstanceOf[A]
    } else {
      super.getControllerInstance(controllerClass)
    }
  }
}
