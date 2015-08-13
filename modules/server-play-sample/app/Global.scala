import java.util.{Timer, TimerTask}

import akka.japi.Option.Some
import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.{FeedService, Context, MemoryFeedStore, AbstractFeedStore}
import controllers.{Deps, Event, EventController, StringController}
import play.api.GlobalSettings
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

import scala.util.Random

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

  //add some dummy values to the stringService
  implicit val context: Context = new Context() {}

  //string service

  val stringService = Deps.stringService

  stringService.push("foo")
  stringService.push(List("bar", "baz"))
  stringService.push("foobar")

  //event service

  val eventService = Deps.eventService
  
  val eventSeq: () => Double = {
    var es = 0.0d
    () => {
      es = es + 1.0d; es
    }
  }

  1 to 25 foreach {
    i => eventService.push(Event(eventSeq(), Some(s"populated at start $i")))
  }

  val timer = new Timer()

  timer.scheduleAtFixedRate(new TimerTask {
    override def run() = {
      eventService.push(Event(eventSeq(), None))
    }
  }, 1000L, 200L)

}
