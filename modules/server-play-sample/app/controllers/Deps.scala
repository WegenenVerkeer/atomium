package controllers

import akka.japi.Option.Some
import be.wegenenverkeer.atomium.format.Url
import be.wegenenverkeer.atomium.server.{Context, FeedService, MemoryFeedStore}

object Deps {

  
  
  lazy val stringService : FeedService[String, Context] = {
    val id = "my_feed"
    val stringStore = new MemoryFeedStore[String, Context](id, Url(s"http://localhost:9000/feeds/$id/"), Some("strings of life"), "text/plain")
    val stringService = new FeedService[String, Context](2, stringStore)
    
    stringService
  }


  lazy val eventService : FeedService[Event,Context] = {
    val events_id = "events"
    val eventStore = new MemoryFeedStore[Event, Context](events_id, Url(s"http://localhost:9000/feeds/$events_id/"), Some("events"), "application/xml")
    new FeedService[Event, Context](10, eventStore)
  }


}
