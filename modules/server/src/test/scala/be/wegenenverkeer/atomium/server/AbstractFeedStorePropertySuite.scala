package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.api.FeedPage
import be.wegenenverkeer.atomium.format.{Link, Url}
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FunSuite, Matchers}

class AbstractFeedStorePropertySuite extends FunSuite with Matchers with FeedStoreTestSupport with GeneratorDrivenPropertyChecks {

  private implicit val context: Context = new Context {}

  def validFeedStores = for {
    n <- Gen.choose(1, 60)
    s <- Gen.choose(0, 2)
  } yield {
    val feedStore = new TestFeedStore[Int, Context]()
    0 to n map { i =>
      feedStore.sequenceNumbersToSkipForPush(s)
      feedStore.push(i)
    }
    feedStore
  }

  def validPageSizes = Gen.choose(1, 11)

  test("head of non-empty feed is correct") {
    forAll(validFeedStores, validPageSizes) { (feedStore: FeedStore[Int, Context], pageSize: Int) =>
      val head: FeedPage[Int] = feedStore.getHeadOfFeed(pageSize)
      head.complete shouldBe false
      head.getEntries.size should be > 0
      head.getEntries.size should be <= pageSize
      head.previousLink.asScala should be(None)
      head.lastLink.asScala shouldEqual Some(new Link(Link.LAST, s"0/forward/$pageSize"))
    }
  }

  test("last page of non-empty feed is correct") {
    forAll(validFeedStores, validPageSizes) { (feedStore: FeedStore[Int, Context], pageSize: Int) =>
      val lastPage: FeedPage[Int] = feedStore.getFeed(0, pageSize, forward = true).get
      lastPage.getEntries.size should be > 0
      lastPage.getEntries.size should be <= pageSize
      lastPage.selfLink shouldEqual new Link(Link.SELF, s"0/forward/$pageSize")
      lastPage.lastLink.asScala shouldEqual Some(new Link(Link.LAST, s"0/forward/$pageSize"))
      lastPage.nextLink.asScala shouldBe None
      //page is complete when there a previous link
      lastPage.complete shouldBe lastPage.previousLink.isPresent
    }
  }

  def getPath(url: Url): (Long, Int, Boolean) = {
    getPath(url.getPath)
  }

  def getPath(url: String): (Long, Int, Boolean) = {
    val path = url.split("/")
    val forward = path(1) == "forward"
    (path(0).toLong, path(2).toInt, forward)
  }

  test("navigate from head to tail") {
    forAll(validFeedStores, validPageSizes) { (feedStore: FeedStore[Int, Context], pageSize: Int) =>
      var page: FeedPage[Int] = feedStore.getHeadOfFeed(pageSize)
      page.getEntries.size should be > 0
      page.getEntries.size should be <= pageSize
      while (page.nextLink.asScala != None) {
        val selfLink = page.selfLink
        val nextLink = page.nextLink.get
        val nextPath = getPath(nextLink.getHref)
        val prevPage = page
        page = feedStore.getFeed(nextPath._1, nextPath._2, nextPath._3).get
        page.getEntries.size shouldEqual pageSize
        page.complete shouldEqual true
        page.previousLink.get.getHref shouldEqual selfLink.getHref
        //check that the previous page is the same as the one we have already visited
        val prevPath = getPath(page.previousLink.get.getHref)
        feedStore.getFeed(prevPath._1, prevPath._2, prevPath._3).get shouldEqual prevPage
      }
    }
  }

  test("navigate from tail to head") {
    forAll(validFeedStores, validPageSizes) { (feedStore: FeedStore[Int, Context], pageSize: Int) =>
      var page: FeedPage[Int] = feedStore.getFeed(0, pageSize, forward = true).get
      page.getEntries.size should be > 0
      while (page.previousLink.asScala != None) {
        page.getEntries.size shouldEqual pageSize
        page.complete shouldEqual true
        val previousLink = page.previousLink.get
        val prevPath = getPath(previousLink.getHref)
        val nextPage = page
        page = feedStore.getFeed(prevPath._1, prevPath._2, prevPath._3).get
        val selfLink = page.selfLink
        previousLink.getHref shouldEqual selfLink.getHref
        //check that the next page is the same as the one we have already visited
        val nextPath = getPath(page.nextLink.get.getHref)
        feedStore.getFeed(nextPath._1, nextPath._2, nextPath._3).get shouldEqual nextPage
      }
    }
  }

}
