package be.wegenenverkeer.atomium.japi.client

import java.io.File

import be.wegenenverkeer.atomium.japi.format.Feed
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers}

class JacksonParserAtomFeedTest extends FlatSpec with Matchers {

  private var mapper = new ObjectMapper
  mapper.registerModule(new JodaModule());

  "A JacksonParser" should "deserialize atom json file" in {

    val json = FileUtils.readFileToString(new
        File(this.getClass.getClassLoader.getResource("be/wegenenverkeer/atomium/japi/client/atom-feed-sample.txt").getFile))

    val feed: Feed[EventFeedEntryTo] = mapper.readValue(json, new TypeReference[Feed[EventFeedEntryTo]]() {})
    feed shouldNot be (null)
    feed.getEntries.get(0).getContent.getValue shouldBe a [EventFeedEntryTo]
  }

}
