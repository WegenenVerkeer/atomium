package be.wegenenverkeer.atom

import org.joda.time.LocalDateTime
import org.scalatest.{FlatSpec, Matchers}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}


class FeedProcessorTest extends FlatSpec with Matchers {

  type StringFeed = Feed[String]
  type Feeds = List[StringFeed]

  case class Scenario(provider:TestFeedProvider,
                      consumedEvents:List[String]) {

    val consumer = new StatefulEntryConsumer
    val process = new FeedProcessor[String](provider, consumer)

    val result = Try(process.start())

    consumedEvents shouldBe consumer.consumedEvents.toList
    lastConsumedEntryId shouldBe consumer.lastConsumedEntryId

    def lastConsumedEntryId:Option[String] = {
      consumedEvents.reverse.headOption
    }

    def assertResult(block:  Try[AtomResult[String]] => Unit) = block(result)

    def shouldSucceed = {

      assertResult { result =>
        result.flatMap(_.asTry).isSuccess shouldBe true
      }

      new {
        def consumingNothing: this.type = {
          result match {
            case Success(AtomNothing) => this
            case anyOther => fail(s"Expecting a AtomNothing, got a $anyOther")
          }
        }
        def lastConsuming(lastConsumedId:String): this.type = {
          result match {
            case Success(AtomSuccess(Some(entry))) => entry.id shouldBe lastConsumedId
            case anyOther => fail(s"Expecting a AtomSuccess($lastConsumedId), got a $anyOther")
          }
          this
        }
      }
    }


    def shouldFail : this.type = {
      assertResult { result =>
        result.flatMap(_.asTry).isFailure shouldBe true
      }
      this
    }
  }


  "FeedProcessor" should "NOT terminate with an error if feed is empty" in {
    Scenario(
      provider = feedProvider(None),
      consumedEvents = List()
    ).shouldSucceed.consumingNothing
  }

  it should "process all entries when starting without an initial entry id" in {
    Scenario(
      provider = feedProvider(
        startingFrom = None,
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),
    
      consumedEvents = List("a1", "b1", "c1", "a2", "b2", "c2", "a3", "b3", "c3")
    ).shouldSucceed.lastConsuming("c3")
  }

  it should "process all remaining entries when starting in the middle of a page" in {
    Scenario(
      provider = feedProvider(
        startingFrom = pos("http://www.example.org/feeds/feed/3/3", "b2"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("c2", "a3", "b3", "c3")
    ).shouldSucceed.lastConsuming("c3")
  }

  it should "process all remaining entries when starting a new page" in {
    Scenario(
      provider = feedProvider(
        startingFrom = pos("http://www.example.org/feeds/feed/3/3", "c2"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a3", "b3", "c3")
    ).shouldSucceed.lastConsuming("c3")
  }

  it should "terminate with an error when started with a non-existent entry" in {
    Scenario(
      provider = feedProvider(
        startingFrom = pos("http://www.example.org/feeds/feed/0/3", "d1"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List()
    ).shouldFail
  }

  it should "process nothing when starting from the last entry" in {
    Scenario(
      provider = feedProvider(
        startingFrom = pos("http://www.example.org/feeds/feed/6/3", "c3"),
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List()
    ).shouldSucceed.consumingNothing
  }

  it should "return a Failure if provider throws an exception" in {

    Scenario(
      provider = feedProviderBogus(
        feed("feed/0/3")("a1", "b1", "c1"),
        feed("feed/3/3")("a2", "b2", "c2"),
        feed("feed/6/3")("a3", "b3", "c3")
      ),

      consumedEvents = List("a1", "b1", "c1")

    ).shouldFail
  }

  it should "return a Failure if consumer throws an exception" in {
    val provider = feedProvider(
      startingFrom = None,
      feed("feed/0/3")("a1", "b1", "c1"),
      feed("feed/3/3")("a2", "b2", "c2"),
      feed("feed/6/3")("a3", "b3", "c3")
    )

    val errorMessage = "Error when consuming Entry"
    val consumer = new EntryConsumer[String] {
      override def apply(eventEntry: Entry[String]): Try[Entry[String]] = {
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

  def pos(url:String, entryId: String) : Option[EntryRef[String]] = {
    Some(EntryRef[String](entryId, Url(url)))
  }


  def feedProvider(startingFrom:Option[EntryRef[String]],
                   feeds:Feed[String]*) = new TestFeedProvider(startingFrom, feeds.toList)

  /**
   * Bogus provider. Never returns the next Feed
   */
  def feedProviderBogus(feeds:Feed[String]*) = new TestFeedProvider(None, feeds.toList) {
    override def fetchFeed(page: String): Try[Feed[String]] = {
      Failure(FeedProcessingException(None, "Can't fetch feed"))
    }
  }

  class TestFeedProvider(val initialEntryRef:Option[EntryRef[String]], feeds:List[Feed[String]]) extends FeedProvider[String] {

    val linkedFeeds = {
      // build links between feeds
      feeds match {
        case x :: xs => linkFeeds(x.selfLink, x, xs)
        case Nil => List()
      }
    }

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
      initialEntryRef match {
        case None => optToTry(linkedFeeds.headOption)
        case Some(position) => fetchFeed(position.url.path)
      }

    }

    /**
     * Return feed whose selfLink equals 'page or Failure
     */
    override def fetchFeed(page: String): Try[Feed[String]] = {
      val feedOpt =  linkedFeeds.find {
        feed => feed.resolveUrl(feed.selfLink.href).path == page
      }
      optToTry(feedOpt)
    }

    private def optToTry(feedOpt:Option[Feed[String]]): Success[Feed[String]] = {

      def emptyFeed = {
        val links = List(Link(Link.selfLink, Url("http://www.example.org/feeds")))
        Feed(
          id = "id",
          base = Url("http://www.example.org/feeds"),
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
