package be.vlaanderen.awv.atom

import org.scalatest.{Matchers, FunSuite}
import play.api.libs.json.{Format, Json}
import com.typesafe.scalalogging.slf4j.Logging
import Formats._

class AtomFeedTest extends FunSuite with Matchers with Logging {

  val url  = Url("http://www.examp.le")
  val link = Link(Link.selfLink, url)

  val content = Content(
    List(Json.obj("name" -> "John", "age" -> 30)),
    "application/json"
  )
  val entry = Entry(content, List(link))

  val feed = Feed("id", url, Some("title"), "update", List(link), List(entry))

  test("must ser/deser a URL") {
    serializeAndDeserialize(url)
  }

  test("must ser/deser a Link") {
    serializeAndDeserialize(link)
  }

  test("must ser/deser a Content") {
    serializeAndDeserialize(content)
  }

  test("must ser/deser an Entry") {
    serializeAndDeserialize(entry)
  }

  test("must ser/deser a Feed") {
    serializeAndDeserialize(feed)
  }

  def serializeAndDeserialize[T: Format](input: T): T = {
    val jsValue = Json.toJson(input)
    // read back

    logger.debug(s"ser/deser test for ${input.getClass.getSimpleName}")
    logger.debug(Json.prettyPrint(jsValue))

    val deser = Json.fromJson[T](jsValue)
    deser.asOpt shouldBe defined
    deser.get
  }


}