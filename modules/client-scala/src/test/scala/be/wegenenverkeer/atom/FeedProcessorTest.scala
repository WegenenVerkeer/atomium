package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime
import org.scalatest.{FlatSpec, Matchers}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}


class FeedProcessorTest extends FlatSpec with Matchers {

  type StringFeed = Feed[String]
  type Feeds = List[StringFeed]

  case class Scenario(provider:TestFeedPageProvider,
                      consumedEvents:List[String],
                      lastConsumedEntryId:Option[String]) {

    val consumer = new StatefulEntryConsumer
    val process = new FeedProcessor[String](provider, consumer)

    provider.isStarted shouldBe false
    val result = process.start()
    provider.isStopped shouldBe true

    consumedEvents shouldBe consumer.consumedEvents.toList
    lastConsumedEntryId shouldBe consumer.lastConsumedEntryId

    def assertResult(block:  AtomResult[String] => Unit) = block(result)

    def processingSucceeds() : this.type = {
      assertResult { result =>
        result.asTry.isSuccess shouldBe true
      }
      this
    }

    def processingFails() : this.type = {
      assertResult { result =>
        result.asTry.isFailure shouldBe true
      }
      this
    }
  }


  "FeedProcessor" should "NOT terminate with an error if feed is empty" in {
    Scenario(
      provider = feedProvider(None),
      consumedEvents = List(),
      lastConsumedEntryId = None
    ).processingSucceeds()
  }

  it should "process all entries without a initial entry id" in {
    Scenario(
      provider = feedProvider(
        initialPosition = None,
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    ).processingSucceeds()
  }

  it should "process all remaining entries when starting in the middle of a page" in {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/3/3", "b2"),
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List("c2", "a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    ).processingSucceeds()
  }

  it should "process all remaining entries when starting a new page" in {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/3/3", "c2"),
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List("a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    ).processingSucceeds()
  }

  it should "terminate with an error when started with a non-existent entry" in {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/0/3", "d1"),
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List(),
      lastConsumedEntryId = None
    ).processingFails()
  }

  it should "process nothing when starting from the last entry" in {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/0/3", "c3"),
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List(),
      lastConsumedEntryId = None
    )
    .processingSucceeds()
    .assertResult { result =>
      result shouldBe a [AtomSuccess[_]]
    }
  }

  it should "return a Failure if provider throws an exception" in {

    Scenario(
      provider = feedProviderBogus(
        "a1", "b1", "c1",
        "a2", "b2", "c2",
        "a3", "b3", "c3"
      ),

      consumedEvents = List("a1", "b1", "c1"),
      lastConsumedEntryId = Some("c1")

    ).processingFails()
  }

  it should "return a Failure if an exception is thrown when consuming an entry" in {
    val provider = feedProvider(
      initialPosition = None,
      "a1", "b1", "c1",
      "a2", "b2", "c2",
      "a3", "b3", "c3"
    )

    val errorMessage = "Error when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def apply(eventEntry: Entry[String]): FeedProcessingResult[String] = {
        throw new RuntimeException(errorMessage)
      }
    }

    val processor = new FeedProcessor[String](provider, consumer)
    val result = processor.start()
    result.asTry.isFailure shouldBe true
    result.asTry.failed.map { error =>
      error shouldBe errorMessage
    }
  }



  def pos(url:String, entryId: String) : Option[FeedEntryRef] = {
    Some(FeedEntryRef(Url(url), entryId))
  }


  def feedProvider(initialPosition:Option[FeedEntryRef],
                   entries:String*) = new TestFeedPageProvider(initialPosition, entries.toList)

  /**
   * Bogus provider. Never returns the next Feed
   */
  def feedProviderBogus(entries:String*) = new TestFeedPageProvider(None, entries.toList) {
    override def fetchFeed(page: String): Try[Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      Failure(FeedProcessingException(None, "Can't fetch feed"))
    }
  }

  class TestFeedPageProvider(val initialEntryRef:Option[FeedEntryRef],
                             entries: List[String]) extends FeedPageProvider[String] {

    private val pageSize = 3

    var linkedFeeds : Feeds = buildLinkedFeeds(entries)

    private def buildLinkedFeeds(entries:List[String]): Feeds = {

      if (entries.isEmpty) {
        List(feed("abc", List()))
      } else {

        val feeds = entries.grouped(pageSize).zipWithIndex.map { case (values, index) =>
          feed(s"abc/$index")(values:_*)
        }.toList

        // build links between feeds
        feeds match {
          case x :: xs => linkFeeds(x.selfLink, x, xs)
          case Nil => List()
        }
      }
    }

    private var _started = false
    
    def isStarted = _started
    def isStopped = !isStarted

    /**
     * add 'last' and 'previous' link to list of Feeds
     */
    @tailrec
    private def linkFeeds(lastLink:Link, current:StringFeed, others: Feeds, acc:Feeds = List()) : Feeds = {

      def addLink(feed:StringFeed, linkLabel:String, link:Link) : StringFeed = {
        feed.copy (
          links = feed.links :+ link.copy(rel = linkLabel)
        )
      }

      // last link must always be present
      val currentWithLastLink = addLink(current, Link.lastLink, lastLink)

      others match {
        case Nil => acc :+ currentWithLastLink
        case x :: xs =>
          val currentWithPreviousLink = addLink(currentWithLastLink, Link.previousLink, x.selfLink)
          linkFeeds(lastLink, x, xs, acc :+ currentWithPreviousLink)
      }

    }

    /**
     * Return first feed or a Failure
     */
    override def fetchFeed(): Try[Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      initialEntryRef match {
        case None => optToTry(linkedFeeds.headOption)
        case Some(entryRef) => fetchFeedPageByEntryId(entryRef.entryId)
      }

    }

    /**
     * Return feed whose selfLink equals 'page or Failure
     */
    override def fetchFeed(page: String): Try[Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      val feedOpt =  linkedFeeds.find {
        feed => feed.resolveUrl(feed.selfLink.href).path == page
      }
      optToTry(feedOpt)
    }

    private def optToTry(feedOpt:Option[Feed[String]]) = {
      feedOpt match {
        case None => Failure(FeedProcessingException(None, "no feed found"))
        case Some(feed) => Success(feed)
      }
    }

    override def start(): Unit = {
      _started = true
    }

    override def stop(): Unit = {
      _started = false
    }

    private def fetchFeedPageByEntryId(entryId: String): Try[Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      val reducedEntries = entries.dropWhile(_ != entryId)

      val feed = reducedEntries match {
        case x :: xs =>
          linkedFeeds = buildLinkedFeeds(reducedEntries)
          linkedFeeds.headOption // we may have something

        case Nil => None // entry not found, let it blow!
      }

      optToTry(feed)
    }

    def feed(url:String)(events:String*) : Feed[String] = {
      val entries = events.map { e =>
        val content = Content[String](e, "")
        Entry[String](e, new LocalDateTime(), content, Nil)
      }
      feed(url, entries.toList)
    }

    def feed(url:String, entries: List[Entry[String]]): Feed[String] = {
      val links = List(Link(Link.selfLink, Url("http://www.example.org/feeds") / url))
      Feed(
        id = "id",
        base = Url("http://www.example.org/feeds"),
        title = Option("title"),
        generator = None,
        updated = new LocalDateTime(),
        links = links,
        entries = entries
      )
    }
  }

}
