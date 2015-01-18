package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime
import org.scalatest.{Matchers, FlatSpec}

import scala.util.{Success, Try}
import scala.concurrent.duration._
import FeedEntryIterator.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global



class FeedEntryIteratorTest extends FlatSpec with Matchers {


  "A FeedEntryIterator" should "iterate over nothing if feed is empty" in new Scenario {

    pushEntries(List())

    iteratorFromStart() should have size 0
  }

  it should "process all entries when starting without an initial entry id" in new Scenario {

    val entries = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    pushEntries(entries)

    iteratorFromStart().entryValues shouldBe entries

  }

  it should "process all remaining entries when starting in the middle of a page" in new Scenario {

    pushEntries("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")

    // take the 4th entry
    val entry = iteratorFromStart().find { entryRef => entryRef.value == "b2" }.get

    entry.value shouldBe "b2"

    // initialize a new iterator starting from the 4th entry
    iteratorStartingFrom(Some(entry)).entryValues shouldBe List("c2", "a3", "b3", "c3")

  }

  it should "process all remaining entries when starting a new page" in new Scenario {

    pushEntries("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")

    // take the 5th entry (end of second page)
    val entry = iteratorFromStart().find { entryRef => entryRef.value == "c2" }.get

    entry.value shouldBe "c2"

    // initialize a new iterator starting from the 5th entry
    iteratorStartingFrom(Some(entry)).entryValues shouldBe List("a3", "b3", "c3")
  }

  it should "terminate with an error when started with a non-existent entry" in new Scenario {

    pushEntries("a1", "b1", "c1")

    val entry = EntryRef[String]("non-existent id", Url("http://www.example.org/feeds/0/forward/3"))

    // initialize a new iterator starting from a non-existent entry
    intercept[NoSuchElementException] {
      iteratorStartingFrom(Some(entry)).toList
    }
  }

  it should "process nothing when starting from the last entry" in new Scenario {

    pushEntries("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")

    // take the last entry
    val entry = iteratorFromStart().toList.last

    println(s"all entries = ${iteratorFromStart().entryValues}")
    entry.value shouldBe "c3"

    // initialize a new iterator starting on the last entry
    iteratorStartingFrom(Some(entry)).entryValues should have size 0
  }



  val baseUrl = "http://www.example.org/feeds/"

  trait Scenario {

    //dummy context for MemoryFeedStore
    implicit val c: Context = new Context {}

    val feedStore = new MemoryFeedStore[String]("test", Url(baseUrl) , Some("test"))
    val feedService = new FeedService[String, Context]("test", 3, (_, _) =>  feedStore)

    def pushEntries(values:String*): Unit = pushEntries(values.toList)
    def pushEntries(values:List[String]): Unit = feedService.push(values)

    def iteratorFromStart(): FeedEntryIterator[String] = {
      new TestFeedProvider(None, feedService).iterator(60 seconds)
    }

    def iteratorStartingFrom(entryRef:Option[EntryRef[String]]): FeedEntryIterator[String] = {
      new TestFeedProvider(entryRef, feedService).iterator(60 seconds)
    }

    def collectEntryValues(iter:FeedEntryIterator[String]): List[String] =
      iter.toList.map( e => e.entry.get.content.value)

    implicit class IterValueCollector(iter : FeedEntryIterator[String]) {
      def entryValues = iter.toList.map( e => e.entry.get.content.value)
    }

    implicit class EntryRefValueCollector(entryRef : EntryRef[String]) {
      def value = entryRef.entry.get.content.value
    }

  }

  class TestFeedProvider[C <: Context](val initialEntryRef:Option[EntryRef[String]], feedService:FeedService[String, C])
                                      (implicit cxt:C) extends FeedProvider[String] {


    /**
     * Return first feed or a Failure
     */
    override def fetchFeed(): Try[Feed[String]] = {
      initialEntryRef match {
        case None => optToTry(fetchLastFeed)
        case Some(position) => fetchFeed(position.url.path)
      }

    }

    private def fetchLastFeed: Option[Feed[String]] = {
      val lastFeed =
        for {
          feed <- feedService.getHeadOfFeed()
          lastUrl <- feed.lastLink
          lastFeed <- getFeedPage(lastUrl.href)
        } yield lastFeed
      lastFeed
    }

    private def getFeedPage(pageUrl:Url): Option[Feed[String]] = {
      val params = pageUrl.path.replaceFirst(baseUrl, "").split("/")
      val page = params(0).toInt
      val isForward = params(1) == "forward"
      val pageSize = params(2).toInt
      feedService.getFeedPage(page, pageSize, forward = isForward)
    }
    /**
     * Return feed whose selfLink equals 'page or Failure
     */
    override def fetchFeed(page: String): Try[Feed[String]] = {
      optToTry(getFeedPage(Url(page)))
    }

    private def optToTry(feedOpt:Option[Feed[String]]): Success[Feed[String]] = {

      def emptyFeed = {
        val links = List(Link(Link.selfLink, Url(baseUrl)))
        Feed(
          id = "N/A",
          base = Url(baseUrl),
          title = Option("title"),
          generator = None,
          updated = new LocalDateTime(),
          links = links,
          entries = List()
        )
      }
      feedOpt.map { feed =>
        Success(feed)
      }.getOrElse(Success(emptyFeed))
    }

  }

}
