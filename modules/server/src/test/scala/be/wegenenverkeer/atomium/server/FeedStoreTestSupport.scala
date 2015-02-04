package be.wegenenverkeer.atomium.server

import be.wegenenverkeer.atomium.format.Url
import org.scalatest.Matchers

/**
 * This trait can be used when testing a specific FeedStore implementation
 */
trait FeedStoreTestSupport extends Matchers {

  def testFeedStorePaging(feedStore: AbstractFeedStore[String], pageSize: Int = 2): Unit = {

    val minId = feedStore.minId.toInt

    //push 2 times the pageSize entries on the feed
    minId + 1 to pageSize * 2 map { i =>
      feedStore.push(i.toString)
    }

    //validate last feed page = oldest page
    val lastPage = feedStore.getFeed(minId, pageSize, forward = true).get
    lastPage.complete shouldBe true
    lastPage.selfLink.href should be(Url(s"$minId/forward/$pageSize"))
    lastPage.lastLink.map(_.href) should be(Some(Url(s"$minId/forward/$pageSize")))
    lastPage.previousLink.map(_.href) should be(Some(Url(s"${minId + pageSize}/forward/$pageSize")))
    lastPage.nextLink.map(_.href) should be(None)
    lastPage.entries.size should be(pageSize)
    //check reverse chronological order of list contents
    lastPage.entries.map(_.content.value) shouldEqual reverseSortedList(pageSize, minId + 1)

    //validate feed page
    val feedPage = feedStore.getFeed(minId + pageSize, pageSize, forward = true).get
    feedPage.complete shouldBe false
    feedPage.selfLink.href should be(Url(s"${minId + pageSize}/forward/$pageSize"))
    feedPage.lastLink.map(_.href) should be(Some(Url(s"$minId/forward/$pageSize")))
    feedPage.previousLink.map(_.href) shouldBe None
    feedPage.nextLink.map(_.href) should be(Some(Url(s"${minId + pageSize + 1}/backward/$pageSize")))
    feedPage.entries.size should be(pageSize)
    //check reverse chronological order of list contents
    feedPage.entries.map(_.content.value) shouldEqual reverseSortedList(2 * pageSize, pageSize + minId + 1)

    //head of feed = first page containing newest entries
    feedStore.getHeadOfFeed(pageSize).get shouldEqual feedPage

    //navigate backwards
    feedStore.getFeed(minId + pageSize + 1, pageSize, forward = false).get shouldEqual lastPage

    //non existing page
    val emptyPage = feedStore.getFeed(minId + 2 * pageSize, pageSize, forward = true) should be(None)

    //push extra element
    feedStore.push(List(minId + 2 * pageSize + 1.toString))
    val newFirstPage = feedStore.getFeed(minId + 2 * pageSize, pageSize, forward = true).get
    newFirstPage.complete shouldBe false
    newFirstPage.entries.size should be(1)
    newFirstPage.selfLink.href should be(Url(s"${minId + 2 * pageSize}/forward/$pageSize"))
    newFirstPage.previousLink.map(_.href) shouldBe None
    newFirstPage.nextLink.map(_.href) should be(Some(Url(s"${minId + 2 * pageSize + 1}/backward/$pageSize")))

    //head of feed = first page containing newest entries
    feedStore.getHeadOfFeed(pageSize).get shouldEqual newFirstPage

    //old first page should be complete now
    val middlePage = feedStore.getFeed(minId + pageSize, pageSize, forward = true).get
    middlePage.complete shouldEqual true
    middlePage.previousLink.map(_.href) should be(Some(Url(s"${minId + 2 * pageSize}/forward/$pageSize")))
    middlePage.nextLink.map(_.href) should be(Some(Url(s"${minId + pageSize + 1}/backward/$pageSize")))

    //navigate backwards
    feedStore.getFeed(minId + 2 * pageSize + 1, pageSize, forward = false).get shouldEqual middlePage
    feedStore.getFeed(minId + pageSize + 1, pageSize, forward = false).get shouldEqual lastPage

  }

  def reverseSortedList(from: Long, to: Long): List[String] = {
    require(from > to)
    (from to to by -1) map {
      i => i.toString
    } toList
  }

}
