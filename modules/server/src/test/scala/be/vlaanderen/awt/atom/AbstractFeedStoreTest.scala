package be.vlaanderen.awt.atom

import be.vlaanderen.awv.atom._
import org.joda.time.LocalDateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.collection.immutable.TreeMap

class AbstractFeedStoreTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val urlBuilder = new UrlBuilder {
    override def base: Url = Url("http://www.example.org")
    override def feedLink(start: Long, count: Int): Url = base / start.toString / count.toString
    override def collectionLink: Url = ???
  }

  class MyFeedStore extends AbstractFeedStore[Int]("test_store", None, urlBuilder) {

    var skip = 1
    var nextSequenceNum = 0L
    var entriesMap : TreeMap[Long, Entry[Int]] = TreeMap.empty

    override def context: Context = ???
    def sequenceNumbersToSkipForPush(skip: Int): Unit = {
      require(skip >= 0)
      this.skip = skip
    }
    override def push(entries: Iterable[Int]): Unit = {
      entries foreach { e =>
        nextSequenceNum += (skip+1)
        entriesMap += (nextSequenceNum -> Entry("id", new LocalDateTime(), Content(e, ""), Nil))
      }
      sequenceNumbersToSkipForPush(0)
    }
    override def maxId: Long = nextSequenceNum
    override def getFeedEntries(start: Long, pageSize: Int): List[Entry[Int]] = {
      var result: List[Entry[Int]] = Nil
      for ((seq, entry) <- entriesMap) {
        if (seq >= start && seq < start+pageSize) {
          result ::= entry
        }
      }
      result
    }
  }

  test("store with missing non-consecutive sequence numbers") {
    val feedStore: MyFeedStore = new MyFeedStore()
    feedStore.push(1)  //stored with sequence number 1
    feedStore.sequenceNumbersToSkipForPush(1)
    feedStore.push(2)  //stored with sequence number 3

    val lastPageOfFeed = feedStore.getFeed(1, 2).get
    //feed contains only single item
    lastPageOfFeed.complete shouldBe true
    lastPageOfFeed.entries.size should be(1)
    lastPageOfFeed.entries.head.content.value should be(1)

    val nextPageOfFeed = feedStore.getFeed(3, 2).get
    nextPageOfFeed.complete shouldBe false
    nextPageOfFeed.entries.size should be(1)
    nextPageOfFeed.entries.head.content.value should be(2)

    var headOfFeed = feedStore.getHeadOfFeed(2).get
    headOfFeed should be(nextPageOfFeed)

    feedStore.sequenceNumbersToSkipForPush(3) //then we should have a completely empty page
    feedStore.push(3)

    val emptyFeedPage = feedStore.getFeed(5, 2).get
    emptyFeedPage.complete shouldBe true
    emptyFeedPage.entries.size should be(0)

    headOfFeed = feedStore.getHeadOfFeed(2).get
    headOfFeed.complete shouldBe false
    headOfFeed.entries.size should be(1)
    headOfFeed.entries.head.content.value should be(3)

  }

}
