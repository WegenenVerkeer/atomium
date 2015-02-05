import _root_.java.util.{Timer, TimerTask}

import akka.japi.Option.Some
import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.{FeedService, Context, MemoryFeedStore, AbstractFeedStore}
import controllers.{Event, EventController, StringController}
import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import scala.util.Random

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

  //add some dummy values to the stringService
  implicit val context: Context = new Context() {}

  //string service

  val id = "my_feed"
  val stringStore = new MemoryFeedStore[String, Context](id, Url(s"http://localhost:9000/feeds/$id/"), Some("strings of life"), "text/plain")
  val stringService = new FeedService[String, Context](id, 2, stringStore)
  val stringController = new StringController(stringService)

  stringService.push("foo")
  stringService.push(List("bar", "baz"))
  stringService.push("foobar")

  //event service

  val events_id = "events"
  val eventStore = new MemoryFeedStore[Event, Context](events_id, Url(s"http://localhost:9000/feeds/$events_id/"), Some("events"), "application/xml")
  val eventService = new FeedService[Event, Context](events_id, 10, eventStore)
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
