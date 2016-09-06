package be.wegenenverkeer.atomium.client

import be.wegenenverkeer.atomium.format.{Link, Url, Feed}
import be.wegenenverkeer.atomium.server.{FeedService, MemoryFeedStore, Context}
import org.joda.time.DateTime

import scala.util.{Success, Try}

trait FeedProviderFixture[E] {

  def pageSize: Int = 10

  def baseUrl: String = "http://feed-iterator-fixture/feeds"


  //dummy context for MemoryFeedStore

  private implicit val context: Context = new Context {}

  lazy val feedStore = new MemoryFeedStore[E, Context]("test", Url(baseUrl), Some("test"), "text/plain")
  lazy val feedService = new FeedService[E, Context](pageSize, feedStore)

  def push(values: E*): Unit = push(values.toList)

  def push(values: List[E]): Unit = feedService.push(values)

  protected class TestFeedProvider extends FeedProvider[E] {

    /**
     * Return first feed or a Failure
     */
    override def fetchFeed(initialEntryRef: Option[EntryRef[E]] = None): Try[Feed[E]] = {
      initialEntryRef match {
        case None           => optToTry(fetchLastFeed)
        case Some(position) => fetchFeed(position.url.path)
      }

    }

    /**
     * Return feed whose selfLink equals 'page or Failure
     */
    override def fetchFeed(page: String): Try[Feed[E]] = {
      optToTry(getFeedPage(Url(page)))
    }

    private def fetchLastFeed: Option[Feed[E]] = {
      val lastFeed =
        for {
          lastUrl <- feedService.getHeadOfFeed().lastLink
          lastFeed <- getFeedPage(lastUrl.href)
        } yield lastFeed
      lastFeed
    }

    private def getFeedPage(pageUrl: Url): Option[Feed[E]] = {
      val params = pageUrl.path.replaceFirst(baseUrl + "/", "").split("/")
      val page = params(0).toInt
      val isForward = params(1) == "forward"
      val pageSize = params(2).toInt
      feedService.getFeedPage(page, pageSize, forward = isForward)
    }


    private def optToTry(feedOpt: Option[Feed[E]]): Success[Feed[E]] = {

      def emptyFeed = {
        val links = List(Link(Link.selfLink, Url(baseUrl)))
        Feed(
          id = "N/A",
          base = Url(baseUrl),
          title = Option("title"),
          generator = None,
          updated = new DateTime(),
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
