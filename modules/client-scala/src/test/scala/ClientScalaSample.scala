import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atom.Marshallers.{JsonUnmarshaller, XmlUnmarshaller}
import be.wegenenverkeer.atom._
import be.wegenenverkeer.atom.providers.PlayWsBlockingFeedProvider
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import support.{JacksonSupport, JaxbSupport}

import scala.util.Success


/**
 * This is a scala atomium client example, which use the PlayWsBlockingFeedProvider to process a feed
 * You MUST first start the atomium server play app
 */
object ClientScalaSample extends FeedUnmarshaller[String] {

  implicit val jaxbContext = JAXBContext.newInstance("be.wegenenverkeer.atom.java")

  val xmlUnmarshaller: XmlUnmarshaller[Feed[String]] = JaxbSupport.jaxbUnmarshaller
                                                       .andThen(JFeedConverters.jFeed2Feed)

  private val objectMapper: ObjectMapper = new ObjectMapper()
  objectMapper.registerModule(new JodaModule)

  implicit val objectReader = objectMapper.reader(new TypeReference[Feed[String]]() {})
  val jsonUnmarshaller: JsonUnmarshaller[Feed[String]] = JacksonSupport.jacksonUnmarshaller
                                                         .andThen(JFeedConverters.jFeed2Feed)

  def main(args: Array[String]) {

    val positionAfterFirstPass = runProcessor(None)
    positionAfterFirstPass.map { pos =>
      println(s"received all entries => position = $pos")
    }

    println("starting process from last feed pos")

    val positionAfterSecondPass = runProcessor(positionAfterFirstPass)
    positionAfterFirstPass.map { pos =>
      println(s"received no extra entries => position has not changed = $pos")

    }

  }

  private def runProcessor(feedPosition:Option[FeedPosition]) : Option[FeedPosition] = {
    val provider = new PlayWsBlockingFeedProvider[String](
      feedUrl =  "http://localhost:9000/feeds/my_feed",
      feedPosition = feedPosition,
      feedUnmarshaller = this
    )

    var lastPos: Option[FeedPosition] = feedPosition

    val processor = new FeedProcessor(
      provider,
      new EntryConsumer[String] {
        override def apply(pos: FeedPosition, entry: Entry[String]): FeedProcessingResult = {
          println(s"received: ${entry.content.value}")
          lastPos = Option(pos)
          Success()
        }
      })

    println("starting process for the first time")
    processor.start()

    lastPos
  }
}