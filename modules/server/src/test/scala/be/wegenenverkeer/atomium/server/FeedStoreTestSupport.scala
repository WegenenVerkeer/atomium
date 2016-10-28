package be.wegenenverkeer.atomium.server

import java.util.Optional

import be.wegenenverkeer.atomium.format.{Link}
import org.scalatest.Matchers

import collection.JavaConverters._

/**
 * This trait can be used when testing a specific FeedStore implementation
 */
trait FeedStoreTestSupport extends Matchers {


  //TODO -- move this in some Java/Scala interop classes.
  class RichOptional[T](jOpt: Optional[T])  {
    def asScala = if (jOpt.isPresent) Some(jOpt.get) else None
  }

  implicit def toRichOptional[T](jOpt: Optional[T]) : RichOptional[T] = new RichOptional(jOpt)


  def testFeedStorePaging[C <: Context](feedStore: AbstractFeedStore[String, C], pageSize: Int = 2)(implicit context: C): Unit = {

    val minId = feedStore.minId.toInt

    //push 2 times the pageSize entries on the feed
    minId + 1 to pageSize * 2 map { i =>
      feedStore.push(i.toString)
    }


    //validate last feed page = oldest page
    val lastPage = feedStore.getFeed(minId, pageSize, forward = true).get
    lastPage.complete shouldBe true
    lastPage.selfLink.getHref should be (s"$minId/forward/$pageSize")
    lastPage.lastLink.asScala.map( (l: Link) => l.getHref ) should be(Some( s"$minId/forward/$pageSize"))
    lastPage.previousLink.asScala.map((l: Link) => l.getHref ) should be(Some(s"${minId + pageSize}/forward/$pageSize"))
    lastPage.nextLink.asScala.map((l: Link) => l.getHref ) should be(None)
    lastPage.getEntries.size should be(pageSize)
    //check reverse chronological order of list contents
    lastPage.getEntries.asScala.map(_.getContent.getValue) shouldEqual reverseSortedList(pageSize, minId + 1)

    //validate feed page
    val feedPage = feedStore.getFeed(minId + pageSize, pageSize, forward = true).get
    feedPage.complete shouldBe false
    feedPage.selfLink.getHref should be(s"${minId + pageSize}/forward/$pageSize")
    feedPage.lastLink.asScala.map((l: Link) => l.getHref ) should be(Some(s"$minId/forward/$pageSize"))
    feedPage.previousLink.asScala.map((l: Link) => l.getHref ) shouldBe None
    feedPage.nextLink.asScala.map((l: Link) => l.getHref ) should be(Some(s"${minId + pageSize + 1}/backward/$pageSize"))
    feedPage.getEntries.size should be(pageSize)
    //check reverse chronological order of list contents
    feedPage.getEntries.asScala.map(_.getContent.getValue) shouldEqual reverseSortedList(2 * pageSize, pageSize + minId + 1)

    //head of feed = first page containing newest entries
    feedStore.getHeadOfFeed(pageSize) shouldEqual feedPage

    //navigate backwards
    feedStore.getFeed(minId + pageSize + 1, pageSize, forward = false).get shouldEqual lastPage

    //non existing page
    val emptyPage = feedStore.getFeed(minId + 2 * pageSize, pageSize, forward = true) should be(None)

    //push extra element
    feedStore.push(List(minId + 2 * pageSize + 1.toString))
    val newFirstPage = feedStore.getFeed(minId + 2 * pageSize, pageSize, forward = true).get
    newFirstPage.complete shouldBe false
    newFirstPage.getEntries.size should be(1)
    newFirstPage.selfLink.getHref should be(s"${minId + 2 * pageSize}/forward/$pageSize")
    newFirstPage.previousLink.asScala.map( (l: Link) => l.getHref) shouldBe None
    newFirstPage.nextLink.asScala.map(  (l: Link) => l.getHref ) should be(Some(s"${minId + 2 * pageSize + 1}/backward/$pageSize"))

    //head of feed = first page containing newest entries
    feedStore.getHeadOfFeed(pageSize) shouldEqual newFirstPage

    //old first page should be complete now
    val middlePage = feedStore.getFeed(minId + pageSize, pageSize, forward = true).get
    middlePage.complete shouldEqual true
    middlePage.previousLink.asScala.map(  (l: Link) => l.getHref ) should be(Some(s"${minId + 2 * pageSize}/forward/$pageSize"))
    middlePage.nextLink.asScala.map(  (l: Link) => l.getHref ) should be(Some(s"${minId + pageSize + 1}/backward/$pageSize"))

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
