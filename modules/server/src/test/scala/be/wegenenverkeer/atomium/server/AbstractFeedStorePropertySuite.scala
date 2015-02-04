package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.{Link, Url, Feed}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class AbstractFeedStorePropertySuite extends FunSuite with Matchers with GeneratorDrivenPropertyChecks {

  def validFeedStores = for {
    n <- Gen.choose(1, 60)
    s <- Gen.choose(0, 2)
  } yield {
    val feedStore = new TestFeedStore[Int]()
    0 to n map { i =>
      feedStore.sequenceNumbersToSkipForPush(s)
      feedStore.push(i)
    }
    feedStore
  }

  def validPageSizes = Gen.choose(1, 11)

  test("head of non-empty feed is correct") {
    forAll (validFeedStores, validPageSizes) { (feedStore: FeedStore[Int], pageSize: Int) =>
      val head: Feed[Int] = feedStore.getHeadOfFeed(pageSize).get
      head.complete shouldBe false
      head.entries.size should be > 0
      head.entries.size should be <= pageSize
      head.previousLink should be (None)
      head.lastLink shouldEqual Some(Link(Link.lastLink, Url(s"0/forward/$pageSize")))
    }
  }

  test("last page of non-empty feed is correct") {
    forAll (validFeedStores, validPageSizes) { (feedStore: FeedStore[Int], pageSize: Int) =>
      val lastPage: Feed[Int] = feedStore.getFeed(0, pageSize, forward = true).get
      lastPage.entries.size should be > 0
      lastPage.entries.size should be <= pageSize
      lastPage.selfLink shouldEqual Link(Link.selfLink, Url(s"0/forward/$pageSize"))
      lastPage.lastLink shouldEqual Some(Link(Link.lastLink, Url(s"0/forward/$pageSize")))
      lastPage.nextLink shouldBe None
      //page is complete when there a previous link
      lastPage.complete shouldBe lastPage.previousLink.isDefined
    }
  }

  def getPath(url: Url): (Long, Int, Boolean) = {
    val path = url.path.split("/")
    val forward = path(1) == "forward"
    (path(0).toLong, path(2).toInt, forward)
  }

  test("navigate from head to tail") {
    forAll (validFeedStores, validPageSizes) { (feedStore: FeedStore[Int], pageSize: Int) =>
      var page: Feed[Int] = feedStore.getHeadOfFeed(pageSize).get
      page.entries.size should be > 0
      page.entries.size should be <= pageSize
      while (page.nextLink != None) {
        val selfLink = page.selfLink
        val nextLink = page.nextLink.get
        val nextPath = getPath(nextLink.href)
        val prevPage = page
        page = feedStore.getFeed(nextPath._1, nextPath._2, nextPath._3).get
        page.entries.size shouldEqual pageSize
        page.complete shouldEqual true
        page.previousLink.get.href shouldEqual selfLink.href
        //check that the previous page is the same as the one we have already visited
        val prevPath = getPath(page.previousLink.get.href)
        feedStore.getFeed(prevPath._1, prevPath._2, prevPath._3).get shouldEqual prevPage
      }
    }
  }

  test("navigate from tail to head") {
    forAll (validFeedStores, validPageSizes) { (feedStore: FeedStore[Int], pageSize: Int) =>
      var page: Feed[Int] = feedStore.getFeed(0, pageSize, forward = true).get
      page.entries.size should be > 0
      while (page.previousLink != None) {
        page.entries.size shouldEqual pageSize
        page.complete shouldEqual true
        val previousLink = page.previousLink.get
        val prevPath = getPath(previousLink.href)
        val nextPage = page
        page = feedStore.getFeed(prevPath._1, prevPath._2, prevPath._3).get
        val selfLink = page.selfLink
        previousLink.href shouldEqual selfLink.href
        //check that the next page is the same as the one we have already visited
        val nextPath = getPath(page.nextLink.get.href)
        feedStore.getFeed(nextPath._1, nextPath._2, nextPath._3).get shouldEqual nextPage
      }
    }
  }

}
