import javax.xml.bind.JAXBContext

import be.vlaanderen.awv.atom.Marshallers.{XmlUnmarshaller, JsonUnmarshaller}
import be.vlaanderen.awv.atom._
import be.vlaanderen.awv.atom.providers.PlayWsBlockingFeedProvider
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import support.{JaxbSupport, JacksonSupport}

import scala.util.Success

/**
 * This is a scala atomium client example, which use the PlayWsBlockingFeedProvider to process a feed
 * You MUST first start the atomium server play app
 */
object ClientScalaSample extends FeedUnmarshaller[String] {

  implicit val jaxbContext = JAXBContext.newInstance("be.vlaanderen.awv.atom")
  val xmlUnmarshaller : XmlUnmarshaller[Feed[String]] = JaxbSupport.jaxbUnmarshaller.andThen(JFeedConverters.jFeed2Feed)

  private val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(new JodaModule)
  implicit val objectReader = objectMapper.reader(new TypeReference[JFeed[String]]() {})
  val jsonUnmarshaller : JsonUnmarshaller[Feed[String]] = JacksonSupport.jacksonUnmarshaller.andThen(JFeedConverters.jFeed2Feed)

  def main(args: Array[String]) {
    val provider: PlayWsBlockingFeedProvider[String] = new PlayWsBlockingFeedProvider[String]("http://localhost:9000/feeds/my_feed", None, this, "application/xml")
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