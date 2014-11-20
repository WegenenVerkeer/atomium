import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.Formats._
import be.vlaanderen.awv.atom.providers.PlayWsBlockingFeedProvider
import play.api.libs.json._

import scala.util.{Failure, Success}

object ClientScalaSample {

  implicit val unmarshaller : FeedEntryUnmarshaller[Int] = {
    responseBody => Success((Json.parse(responseBody) \ "feed").as[Feed[Int]])
  }

  def main(args: Array[String]) {
    val provider: PlayWsBlockingFeedProvider[Int] = new PlayWsBlockingFeedProvider[Int]("http://localhost/foo", None)
    provider.start()
    provider.fetchFeed() match {
      case Success(feed) => print(feed)
      case Failure(exc) => print(exc.getMessage)
    }
    provider.stop()
  }
}