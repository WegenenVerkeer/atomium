package be.wegenenverkeer.atomium.client

import java.lang
import java.util.function.{BiFunction => JBiFunction}

import be.wegenenverkeer.atomium.japi.client.{RxHttpAtomiumClient => JAtomiumClient, FeedEntry, RetryStrategy => JRetryStrategy}
import JAtomiumClient.{AtomiumFeed => JFeedObservableBuilder}
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable


trait RetryStrategy {
  def apply(count: Int, exception: Throwable): Long
}

/**
 * Scala AtomiumClient.
 *
 * @see [[JAtomiumClient]]
 */
class AtomiumClient(val inner: JAtomiumClient) {

  def feed[E](path: String, entryTypeMarker: Class[E]) = new FeedObservableBuilder(inner.feed[E](path, entryTypeMarker))

  class Builder extends JAtomiumClient.Builder

}

class FeedObservableBuilder[E](val inner: JFeedObservableBuilder[E]) {



//  private def toJavaFunction(f: (Int,Throwable) => Long) = new JBiFunction[Integer,Throwable,java.lang.Long] {
//    override def apply(a: Integer, b: Throwable): java.lang.Long = f(a,b)
//  }

  private def toJRetryStrategy(retryStrategy: RetryStrategy) : JRetryStrategy = new JRetryStrategy {
    override def apply(count: Integer, exception: Throwable): lang.Long = retryStrategy(count, exception)
  }

  def withRetry( strategy: RetryStrategy ) : FeedObservableBuilder[E] = {
    new FeedObservableBuilder( this.inner.withRetry( toJRetryStrategy(strategy)) )
  }

  /**
   * Creates a "cold" [[rx.lang.scala.Observable]] that, when subscribed to, emits all entries in the feed
   * starting from the oldest entry immediately after the specified entry.
   *
   * <p>
   * When subscribed to, the observable will create a single-threaded [[rx.Scheduler.Worker]] that will:
   * </p>
   * <ul>
   * <li>retrieve the specified feed page</li>
   * <li>emit all entries more recent that the specified feed entry</li>
   * <li>follow iteratively the 'previous'-links and emits all entries on the linked-to pages until it
   * arrives at the head of the feed (identified by not having a 'previous'-link)</li>
   * <li>poll the feed at the specified interval (using conditional GETs) and emit all entries not yet seen</li>
   * </ul>
   * <p>The worker will exit only on an error condition, or on unsubscribe.</p>
   * <p><em>Important:</em> a new and independent worker is created for each subscriber.</p>
   *
   * @param entryId      the entry-id of an entry on the specified page
   * @param pageUrl      the url (absolute, or relative to the feed's base url) of the feed-page, containing the entry
   *                     identified with the entryId argument
   * @param intervalInMs the polling interval in milliseconds.
   * @return an Observable emitting all entries since the specified entry
   */
  def observeFrom(entryId: String, pageUrl: String, intervalInMs: Int) = toScalaObservable(inner
    .observeFrom(entryId, pageUrl, intervalInMs))


  /**
   * @deprecated Will be replaced by observeFrom() in next version
   */
  def observeSince(entryId: String, pageUrl: String, intervalInMs: Int) = observeFrom(entryId, pageUrl, intervalInMs)

  /**
   * Creates a "cold" [[rx.lang.scala.Observable]] that, when subscribed to, emits all entries on the feed
   * starting from those then on the head of the feed.
   * <p>The behavior is analogous to the method {@code observeFrom()} but starting from the head page</p>
   * @param intervalInMs the polling interval in milliseconds.
   *
   * @return a "cold" Observable
   */
  def observeFromNowOn(intervalInMs: Int): Observable[FeedEntry[E]] = toScalaObservable(inner.observeFromNowOn(intervalInMs))

  /**
   * Creates a "cold" [[rx.lang.scala.Observable]] that, when subscribed to, emits all entries on the feed
   * starting from the begnning.
   *
   * <p>
   * Starting from the beginning means going to the 'last' page of the feed, and the bottom entry on that page, and working back
   * to the present.
   * </p>
   *
   * @return a "cold" Observable
   */
  def observeFromBeginning(intervalInMs: Int) = toScalaObservable(inner.observeFromBeginning(intervalInMs))
}


object ImplicitConversions {

  import rx.lang.scala.JavaConversions._


  trait JavaAtomiumClientWrapper {

    val inner: JAtomiumClient

    def asScala: AtomiumClient = new AtomiumClient(inner)
  }

  implicit def wrap(javaClient: JAtomiumClient): JavaAtomiumClientWrapper = new JavaAtomiumClientWrapper {
    val inner = javaClient
  }

  implicit def unwrap(client: AtomiumClient): JAtomiumClient = client.inner

  implicit def func2RetryStrategy( f : (Int, Throwable) => Long) : RetryStrategy = new RetryStrategy {
    override def apply(count: Int, exception: Throwable): Long = f(count, exception)
  }


}
