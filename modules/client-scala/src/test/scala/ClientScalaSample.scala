import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.format.Entry
import be.vlaanderen.awv.atom.providers.PlayWsBlockingFeedProvider
import com.sun.jersey.api.json.{JSONConfiguration, JSONJAXBContext}

import scala.collection.JavaConversions._
import scala.util.Success

/**
 * This is a scala atomium client example, which use the PlayWsBlockingFeedProvider to process a feed
 * You MUST first start the atomium server play app
 */
object ClientScalaSample {

  val config: JSONConfiguration = JSONConfiguration.mapped.rootUnwrapping(true).
    xml2JsonNs(Map("http://www.w3.org/2005/Atom" -> "", "http://www.w3.org/XML/1998/namespace" -> "")).arrays("link", "entry").build
  implicit val jsonJaxbContext = new JSONJAXBContext(config, "be.vlaanderen.awv.atom.jformat")

  def main(args: Array[String]) {
    val provider: PlayWsBlockingFeedProvider[String] = new PlayWsBlockingFeedProvider[String]("http://localhost:9000/feeds/my_feed", None)
    var lastPos: Option[FeedPosition] = None
    val processor = new FeedProcessor(provider, new EntryConsumer[String] {
      override def apply(pos: FeedPosition, entry: Entry[String]): FeedProcessingResult = {
        println(s"received: ${entry.content.value}")
        lastPos = Option(pos)
        Success()
      }
    })
    println("starting process for the first time")
    processor.start()
    println(s"received all entries => position = ${lastPos.map(_.toString).getOrElse("no position")}")
    provider.feedPosition = lastPos
    println("starting process from last feed pos")
    processor.start()
    println(s"received no extra entries => position has not changed = ${lastPos.map(_.toString).getOrElse("no position")}")

    provider.stop()
  }
}