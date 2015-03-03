package be.wegenenverkeer.atomium.japi.client

import be.wegenenverkeer.atomium.client._
import be.wegenenverkeer.atomium.format.FeedConverters._
import be.wegenenverkeer.atomium.japi.format.Feed
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

/**
 * NOTE: this test exist only to ensure that everything is wrapped/wired as expected.
 *
 * No need for extensive testing because the java implementation is
 * a thin layer on top of the Scala `FeedEntryIterator` which is exhaustively tests in `FeedEntryIteratorTest`.
 */
class FeedEntryJavaIteratorTest extends FlatSpec with Matchers {

  "A java FeedEntryIterator" should "iterate over all entries when starting without an initial entry id" in
    new Scenario {

      val entries = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
      push(entries: _*)

      // build a java feed entry iterable backed by a java feed provide
      val iterable = new FeedEntryIterable[String](newJavaFeedProvider)

      val entryValues =
        for {e <- iterable.asScala}
        yield e.entry.get.content.value

      entryValues shouldBe entries

    }


  trait Scenario extends FeedProviderFixture[String] {

    /**
     * Builds a java wrapper around the Scala `FeedProvider` defined in `FeedProviderFixture`
     */
    def newJavaFeedProvider = new FeedProvider[String] {

      val underlying = new TestFeedProvider()

      override def fetchFeed(entryRef: EntryRef[String]): Feed[String] =
        underlying.fetchFeed(Option(entryRef)).get.asJava

      override def fetchFeed(): Feed[String] =
        underlying.fetchFeed(None).get.asJava

      override def fetchFeed(page: String): Feed[String] =
        underlying.fetchFeed(page).get.asJava
    }


  }

}
