package be.vlaanderen.awv.atom


import org.scalatest.{Matchers, FunSuite}
import scala.annotation.tailrec
import scala.collection.mutable
import scalaz.Scalaz._
import scalaz._


class FeedProcessorTest extends FunSuite with Matchers {

  type StringFeed = Feed[String]
  type Feeds = List[StringFeed]



  class StatefulEntryConsumer extends EntryConsumer[String] {
    var finalPosition:Option[FeedPosition] = None
    var consumedEvents = new mutable.ListBuffer[String]

    override def consume(position: FeedPosition, eventEntry: Entry[String]): FeedProcessingResult = {
      finalPosition = Option(position)
      eventEntry.content.value.foreach { e =>
        consumedEvents += e
      }
      position.success[FeedProcessingError]
    }
  }


  case class Scenario(provider:TestFeedProvider,
                      consumedEvents:List[String],
                      initialPosition:Option[FeedPosition] = None,
                      finalPosition:Option[FeedPosition]) {

    val consumer = new StatefulEntryConsumer
    val process = new FeedProcessor[String](initialPosition, provider, consumer)

    provider.isStarted shouldBe false
    val result = process.start()
    provider.isStopped shouldBe true

    consumedEvents shouldBe consumer.consumedEvents.toList
    finalPosition shouldBe consumer.finalPosition



    def assertResult(block: FeedProcessingResult => Unit) = block(result)
  }


  test("Feed is empty") {
    Scenario(
      provider = feedProvider(),
      consumedEvents = List(),
      initialPosition = None,
      finalPosition = None
    ) assertResult { result =>
      result.isFailure shouldBe true
    }
  }

  test("Feed is consumed from begin to end") {
    Scenario(
      provider = feedProvider(
        feed("/feed/1")("a1", "b1", "c1"),
        feed("/feed/2")("a2", "b2", "c2"),
        feed("/feed/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3"),
      finalPosition = Some(FeedPosition(link("/feed/3"), 2))
    )
  }

  test("Feed is consumed from position [/feed/2,1] until end") {
    Scenario(
      provider = feedProvider(
        feed("/feed/1")("a1", "b1", "c1"),
        feed("/feed/2")("a2", "b2", "c2"),
        feed("/feed/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("c2", "a3", "b3", "c3"),
      initialPosition = Some(FeedPosition(link("/feed/2"), 1)),
      finalPosition = Some(FeedPosition(link("/feed/3"), 2))
    )
  }

  test("Feed is consumed from position [/feed/2,2] until end") {
    Scenario(
      provider = feedProvider(
        feed("/feed/1")("a1", "b1", "c1"),
        feed("/feed/2")("a2", "b2", "c2"),
        feed("/feed/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a3", "b3", "c3"),
      initialPosition = Some(FeedPosition(link("/feed/2"), 2)),
      finalPosition = Some(FeedPosition(link("/feed/3"), 2))
    )
  }


  test("Feed is consumed from position [/feed/2,10] until end. Latest successful position is wrong, it's outside feed") {
    Scenario(
      provider = feedProvider(
        feed("/feed/1")("a1", "b1", "c1"),
        feed("/feed/2")("a2", "b2", "c2"),
        feed("/feed/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a3", "b3", "c3"),
      initialPosition = Some(FeedPosition(link("/feed/2"), 10)),
      finalPosition = Some(FeedPosition(link("/feed/3"), 2))
    )
  }

  test("Error while fetching next Feed") {
    Scenario(
      provider = feedProviderBogus(
        feed("/feed/1")("a1", "b1", "c1"),
        feed("/feed/2")("a2", "b2", "c2"),
        feed("/feed/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a1", "b1", "c1"),
      initialPosition = None,
      finalPosition = Some(FeedPosition(link("/feed/1"), 2))
    ) assertResult { result =>
      result.isFailure shouldBe true
    }
  }

  test("Error when consuming Entry") {
    val provider = feedProvider(
      feed("/feed/1")("a1", "b1", "c1"),
      feed("/feed/2")("a2", "b2", "c2"),
      feed("/feed/3")("a3", "b3", "c3")
    )

    val foutMelding = "Error when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def consume(position: FeedPosition, eventEntry: Entry[String]): FeedProcessingResult = {
        FeedProcessingError(Option(position), foutMelding).failure[FeedPosition]
      }
    }

    val processor = new FeedProcessor[String](None, provider, consumer)
    val result = processor.start()
    result.isFailure shouldBe true
    result.swap.map { error =>
      error.message shouldBe foutMelding
    }
  }

  test("Exception when consuming Entry is wrapped on Validation failure") {
    val provider = feedProvider(
      feed("/feed/1")("a1", "b1", "c1"),
      feed("/feed/2")("a2", "b2", "c2"),
      feed("/feed/3")("a3", "b3", "c3")
    )

    val foutMelding = "Exception when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def consume(position: FeedPosition, eventEntry: Entry[String]): FeedProcessingResult = {
        throw new RuntimeException(foutMelding)
      }
    }

    val processor = new FeedProcessor[String](None, provider, consumer)
    val result = processor.start()
    result.isFailure shouldBe true
    result.swap.map { error =>
      error.message shouldBe foutMelding
    }
  }

  def feed(url:String)(events:String*) : Feed[String] = {
    val entries = events.map { e =>
      val content = Content[String](List(e), e)
      Entry[String](content, List())
    }

    val links = List(Link(Link.selfLink, Url(url)))
    Feed("id", Url("base"), Option("title"), "update", links, entries.toList)
  }

  def link(url:String) : Link = Link(Link.selfLink, Url(url))

  def feedProvider(feeds:Feed[String]*) = new TestFeedProvider(feeds.toList)

  /**
   * Bogus provider. Never returns the next Feed
   */
  def feedProviderBogus(feeds:Feed[String]*) = new TestFeedProvider(feeds.toList) {
    override def fetchFeed(page: String): Validation[FeedProcessingError, Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      FeedProcessingError(None, "Can't fetch feed").failure[Feed[String]]
    }
  }

  class TestFeedProvider(feeds: Feeds) extends FeedProvider[String] {

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
     * add 'first' and 'next' link to list of Feeds
     */
    @tailrec
    private def linkFeeds(firstLink:Link, current:StringFeed, others: Feeds, acc:Feeds = List()) : Feeds = {

      def addLink(feed:StringFeed, linkLabel:String, link:Link) : StringFeed = {
        feed.copy (
          links = feed.links :+ link.copy(rel = linkLabel)
        )
      }

      // first link must always be present
      val currentWithFirstLink = addLink(current, Link.firstLink, firstLink)

      others match {
        case Nil => acc :+ currentWithFirstLink
        case x :: xs =>
          val currentWithNextLink = addLink(currentWithFirstLink, Link.nextLink, x.selfLink)
          linkFeeds(firstLink, x, xs, acc :+ currentWithNextLink)
      }

    }

    /**
     * Return first feed or a Failure
     */
    override def fetchFeed(): Validation[FeedProcessingError, Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      optToValidation(linkedFeeds.headOption)
    }

    /**
     * Return feed whose selfLink equals 'page or Failure
     */
    override def fetchFeed(page: String): Validation[FeedProcessingError, Feed[String]] = {
      assert(isStarted, "Provider must be managed")
      val feedOpt =  linkedFeeds.find {
        feed => feed.selfLink.href.path == page
      }
      optToValidation(feedOpt)
    }

    private def optToValidation(feedOpt:Option[Feed[String]]) = {
      feedOpt match {
        case None => FeedProcessingError(None, "no feed found").failure[Feed[String]]
        case Some(feed) => feed.success[FeedProcessingError]
      }
    }

    override def start(): Unit = {
      _started = true
    }

    override def stop(): Unit = {
      _started = false
    }

  }

}
