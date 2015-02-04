package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.{Url, Link}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class AbstractFeedStoreTest extends FunSuite with FeedStoreTestSupport with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("empty store") {
    val feedStore = new TestFeedStore[Int]()
    feedStore.getHeadOfFeed(1) shouldBe None
    feedStore.getHeadOfFeed(3) shouldBe None
    intercept[IllegalArgumentException] {
      feedStore.getHeadOfFeed(0)
    }
    feedStore.getFeed(0, 2, forward = true) shouldBe None
    feedStore.getFeed(10, 2, forward = false) shouldBe None
    intercept[IllegalArgumentException] {
      feedStore.getFeed(10, 0, forward = false)
    }
  }

  test("invalid feed retrieval") {
    val feedStore = new TestFeedStore[Int]()
    feedStore.push(1) //stored with sequence number 1
    feedStore.push(2) //stored with sequence number 2
    feedStore.push(3) //stored with sequence number 3

    //pageSize = 1
    feedStore.getFeed(0, 1, forward = true) shouldNot be(None)
    feedStore.getFeed(1, 1, forward = true) shouldNot be(None)
    feedStore.getFeed(2, 1, forward = true) shouldNot be(None)
    feedStore.getFeed(3, 1, forward = true) should be(None)

    feedStore.getFeed(4, 1, forward = false) should be(None)
    //can only retrieve two pages by navigating backwards
    feedStore.getFeed(3, 1, forward = false) shouldNot be(None)
    feedStore.getFeed(2, 1, forward = false) shouldNot be(None)
    feedStore.getFeed(1, 1, forward = false) should be(None)
    feedStore.getFeed(0, 1, forward = false) should be(None)

    //pageSize = 2
    feedStore.getFeed(0, 2, forward = true) shouldNot be(None)
    feedStore.getFeed(1, 2, forward = true) should be(None)
    feedStore.getFeed(2, 2, forward = true) shouldNot be(None)
    feedStore.getFeed(3, 2, forward = true) should be(None)
    feedStore.getFeed(4, 2, forward = true) should be(None)

    feedStore.getFeed(5, 2, forward = false) should be(None)
    feedStore.getFeed(4, 2, forward = false) should be(None)
    //can only retrieve last page by navigating backwards
    feedStore.getFeed(3, 2, forward = false) shouldNot be(None)
    feedStore.getFeed(2, 2, forward = false) should be(None)
    feedStore.getFeed(1, 2, forward = false) should be(None)
    feedStore.getFeed(0, 2, forward = false) should be(None)

    //pageSize = 3
    feedStore.getFeed(0, 3, forward = true) shouldNot be(None)
    feedStore.getFeed(1, 3, forward = true) should be(None)
    feedStore.getFeed(2, 3, forward = true) should be(None)
    feedStore.getFeed(3, 3, forward = true) should be(None) //feed contains only single page

    //can not retrieve any pages by navigating backwards
    feedStore.getFeed(4, 3, forward = false) should be(None)
    feedStore.getFeed(3, 3, forward = false) should be(None)
    feedStore.getFeed(2, 3, forward = false) should be(None)
    feedStore.getFeed(1, 3, forward = false) should be(None)
    feedStore.getFeed(0, 3, forward = false) should be(None)

    //pageSize = 4
    feedStore.getFeed(0, 4, forward = true) shouldNot be(None)
    feedStore.getFeed(1, 4, forward = true) should be(None)
    feedStore.getFeed(2, 4, forward = true) should be(None)
    feedStore.getFeed(3, 4, forward = true) should be(None)
    feedStore.getFeed(4, 4, forward = true) should be(None)

    //can not retrieve any pages by navigating backwards
    feedStore.getFeed(5, 4, forward = false) should be(None)
    feedStore.getFeed(4, 4, forward = false) should be(None)
    feedStore.getFeed(3, 4, forward = false) should be(None)
    feedStore.getFeed(2, 4, forward = false) should be(None)
    feedStore.getFeed(1, 4, forward = false) should be(None)
  }

  test("store with consecutive sequence numbers") {
    testFeedStorePaging(feedStore = new TestFeedStore[String], pageSize = 5)
  }

  test("store with missing non-consecutive sequence numbers") {
    val feedStore = new TestFeedStore[Int]()
    feedStore.push(1)  //stored with sequence number 1
    feedStore.sequenceNumbersToSkipForPush(1)
    feedStore.push(2)  //stored with sequence number 3
    feedStore.push(3)  //stored with sequence number 4
    feedStore.sequenceNumbersToSkipForPush(3)
    feedStore.push(4)  //stored with sequence number 8
    feedStore.push(5)  //stored with sequence number 9

    //move forwards with pageSize 5 from tail
    val lastPageOfFeedWithSize5 = feedStore.getFeed(0, 5, forward = true).get
    lastPageOfFeedWithSize5.complete shouldBe false //there is no previous feed page (yet)
    lastPageOfFeedWithSize5.entries.size should be(5)
    lastPageOfFeedWithSize5.entries.map( _.content.value ) should be(List(5, 4, 3, 2, 1))
    lastPageOfFeedWithSize5.previousLink shouldBe None
    lastPageOfFeedWithSize5.nextLink shouldBe None
    lastPageOfFeedWithSize5.selfLink should be (Link(Link.selfLink, Url("0/forward/5")))

    //since only 1 page in feed => head equals last
    feedStore.getHeadOfFeed(5).get shouldEqual lastPageOfFeedWithSize5

    feedStore.getFeed(9, 5, forward = true) shouldBe None

    //move forwards with pageSize 2 from tail
    val lastPageOfFeedWithSize2 = feedStore.getFeed(0, 2, forward = true).get
    lastPageOfFeedWithSize2.complete shouldBe true
    lastPageOfFeedWithSize2.entries.size should be(2)
    lastPageOfFeedWithSize2.entries.map( _.content.value ) should be(List(2, 1))
    lastPageOfFeedWithSize2.previousLink should be(Some(Link(Link.previousLink, Url("3/forward/2"))))
    lastPageOfFeedWithSize2.nextLink shouldBe None
    lastPageOfFeedWithSize2.selfLink should be(Link(Link.selfLink, Url("0/forward/2")))

    //moving forward => previous page
    val middlePageOfFeedWithSize2 = feedStore.getFeed(3, 2, forward = true).get
    middlePageOfFeedWithSize2.complete shouldBe true
    middlePageOfFeedWithSize2.entries.size should be(2)
    middlePageOfFeedWithSize2.entries.map( _.content.value ) should be(List(4, 3))
    middlePageOfFeedWithSize2.previousLink should be(Some(Link(Link.previousLink, Url("8/forward/2"))))
    middlePageOfFeedWithSize2.nextLink should be(Some(Link(Link.nextLink, Url("4/backward/2"))))
    middlePageOfFeedWithSize2.selfLink should be(Link(Link.selfLink, Url("3/forward/2")))

    //moving forward => previous page
    val firstPageOfFeedWithSize2 = feedStore.getFeed(8, 2, forward = true).get
    firstPageOfFeedWithSize2.complete shouldBe false
    firstPageOfFeedWithSize2.entries.size should be(1)
    firstPageOfFeedWithSize2.entries.map( _.content.value ) should be(List(5))
    firstPageOfFeedWithSize2.previousLink shouldBe None
    firstPageOfFeedWithSize2.nextLink should be(Some(Link(Link.nextLink, Url("9/backward/2"))))
    firstPageOfFeedWithSize2.selfLink should be(Link(Link.selfLink, Url("8/forward/2")))

    //we are at the head of the feed
    feedStore.getHeadOfFeed(2).get shouldEqual firstPageOfFeedWithSize2

    //moving backwards
    feedStore.getFeed(9, 2, forward =  false).get shouldEqual middlePageOfFeedWithSize2
    feedStore.getFeed(4, 2, forward =  false).get shouldEqual lastPageOfFeedWithSize2

  }

}
