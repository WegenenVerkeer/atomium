package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime
import org.scalatest.{FunSuite, Matchers}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}


class FeedProcessorTest extends FunSuite with Matchers {

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
  }



  test("Feed is empty") {
    Scenario(
      provider = feedProvider(None),
      consumedEvents = List(),
      lastConsumedEntryId = None
    ) assertResult { result =>
      result.asTry.isSuccess shouldBe true
    }
  }

  test("Feed is consumed from begin to end") {
    Scenario(
      provider = feedProvider(
        initialPosition = None,
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    )
  }

  ignore("Feed is consumed from entry 'b2' until end") {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/3/3", "b2"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("c2", "a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    )
  }

  ignore("Feed is consumed from entry 'c2' until end") {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/3/3", "c2"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a3", "b3", "c3"),
      lastConsumedEntryId = Some("c3")
    )
  }


  ignore("Feed is NOT consumed since 'd1' entry does NOT exist") {
    Scenario(
      provider = feedProvider(
        initialPosition = pos("http://www.example.org/feeds/feed/3/3", "d1"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a3", "b3", "c3"),
      lastConsumedEntryId = None
    )
  }

  ignore("Error while fetching next Feed") {
    Scenario(
      provider = feedProviderBogus(
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a1", "b1", "c1"),
      lastConsumedEntryId = Some("c1")

    ) assertResult { result =>
      result.asTry.isFailure shouldBe true
    }
  }

  test("Error when consuming Entry") {
    val provider = feedProvider(initialPosition = None,
      feed("/feed/0/3")("a1", "b1", "c1"),
      feed("/feed/3/3")("a2", "b2", "c2"),
      feed("/feed/6/3")("a3", "b3", "c3")
    )

    val errorMessage = "Error when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def apply(eventEntry: Entry[String]): FeedProcessingResult[String] = {
        Failure(FeedProcessingException(None, errorMessage))
      }
    }

    val processor = new FeedProcessor[String](provider, consumer)
    val result = processor.start()
    result.asTry.isFailure shouldBe true
    result.asTry.failed.map { error =>
      error shouldBe errorMessage
    }
  }

  test("Exception when consuming Entry is wrapped on a Failure") {
    val provider = feedProvider(initialPosition = None,
      feed("/feed/0/3")("a1", "b1", "c1"),
      feed("/feed/3/3")("a2", "b2", "c2"),
      feed("/feed/6/3")("a3", "b3", "c3")
    )

    val errorMessage = "Exception when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def apply(eventEntry: Entry[String]): FeedProcessingResult[String] = {
        throw new RuntimeException(errorMessage)
      }
    }

    val processor = new FeedProcessor[String](provider, consumer)
    val result = processor.start()
    result.asTry.isFailure shouldBe true
    result.asTry.map { error =>
      error shouldBe errorMessage
    }
  }

  def feed(url:String)(events:String*) : Feed[String] = {
    val entries = events.map { e =>
      val content = Content[String](e, "")
      Entry[String](e, new LocalDateTime(), content, Nil)
    }

    val links = List(Link(Link.selfLink, Url("http://www.example.org/feeds") / url))

    Feed(
      id = "id",
      base = Url("http://www.example.org/feeds"),
      title = Option("title"),
      generator = None,
      updated = new LocalDateTime(),
      links = links,
      entries = entries.toList
    )
  }

  def pos(url:String, entryId: String) : Option[FeedPosition] = {
    Some(FeedPosition(Url(url), Option(entryId)))
  }
  def pos(url:String) : Option[FeedPosition] = {
    Some(FeedPosition(Url(url), None))
  }


  def feedProvider(initialPosition:Option[FeedPosition],
                   feeds:Feed[String]*) = new TestFeedPageProvider(initialPosition, feeds.toList)

  /**
   * Bogus provider. Never returns the next Feed
   */
  def feedProviderBogus(feeds:Feed[String]*) = new TestFeedPageProvider(None, feeds.toList) {
    override def fetchFeed(page: String): Try[Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      Failure(FeedProcessingException(None, "Can't fetch feed"))
    }
  }

  class TestFeedPageProvider(initialPos:Option[FeedPosition],
                         feeds: Feeds) extends FeedPageProvider[String] {

    val linkedFeeds = {
      // build links between feeds
      feeds match {
        case x :: xs => linkFeeds(x.selfLink, x, xs)
        case Nil => List()
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
      initialPosition match {
        case None => optToTry(linkedFeeds.headOption)
        case Some(position) => fetchFeed(position.url.path)
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

    override def initialPosition: Option[FeedPosition] = initialPos
  }

}
