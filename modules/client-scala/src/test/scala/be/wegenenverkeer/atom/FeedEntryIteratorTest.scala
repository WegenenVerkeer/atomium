package be.wegenenverkeer.atom

import be.wegenenverkeer.atomium.format.Url
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._


class FeedEntryIteratorTest extends FlatSpec with Matchers {


  "A FeedEntryIterator" should "iterate over all entries when starting without an initial entry id" in new Scenario {

    val entries = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    push(entries:_*)

    iteratorFromStart.entryValues shouldBe entries

  }

  it should  "iterate over nothing if feed is empty" in new Scenario {
    // NOTE: no entries are pushed, feed store is thus empty
    iteratorFromStart should have size 0
  }


  it should "iterate over all remaining entries when starting in the middle of a page" in new Scenario {

    push("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    // initialize a new iterator starting from the 4th entry
    iteratorStartingFrom("b2").entryValues shouldBe List("c2", "a3", "b3", "c3")

  }

  it should "iterate over all remaining entries when starting a new page" in new Scenario {

    push("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    // initialize a new iterator starting from the 5th entry
    iteratorStartingFrom("c2").entryValues shouldBe List("a3", "b3", "c3")
  }

  it should "terminate with an error when started with a non-existent entry" in new Scenario {

    push("a1", "b1", "c1")
    val fakeEntry = Some(EntryRef[String]("non-existent-id", Url(baseUrl + "/0/forward/3")))
    // initialize a new iterator starting from a non-existent entry
    intercept[NoSuchElementException] {
      iteratorStartingFrom(fakeEntry).toList
    }
  }

  it should "iterate over nothing when starting from the last entry" in new Scenario {

    push("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    // initialize a new iterator starting on the last entry
    iteratorStartingFrom("c3").entryValues should have size 0
  }


  trait Scenario extends FeedIteratorFixture[String] {

    override val pageSize = 3

    def collectEntryValues(iter: FeedEntryIterator[String]): List[String] =
      iter.toList.map(e => e.entry.get.content.value)

    implicit class IterValueCollector(iter: FeedEntryIterator[String]) {
      def entryValues = iter.toList.map(e => e.entry.get.content.value)
    }

  }

}