import be.vlaanderen.awv.atom._
import controllers.{MemoryFeedStore, MyFeedController}
import play.api.GlobalSettings

object Global extends GlobalSettings {

  val id = "my_feed"
  val feedStore: FeedStore[String] = new MemoryFeedStore[String](id, Url("http://localhost:9000/feeds"), Some("strings of life"))
  val feedService = new FeedService[String, Context](id, 2, { (s, c) => feedStore })
  val feedController = new MyFeedController(feedService)

  //add some dummy values to the feedservice
  implicit val c: Context = new Context() {}
  feedService.push("foo")
  feedService.push(List("bar", "baz"))
  feedService.push("foobar")

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    if (controllerClass == classOf[MyFeedController]) {
      feedController.asInstanceOf[A]
    } else {
      super.getControllerInstance(controllerClass)
    }
  }
}
