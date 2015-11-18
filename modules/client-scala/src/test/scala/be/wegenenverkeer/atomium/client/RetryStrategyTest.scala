package be.wegenenverkeer.atomium.client

import java.util.concurrent.TimeUnit

import be.wegenenverkeer.atomium.japi.client.FeedEntry
import org.scalatest.{Matchers, FlatSpec}
import rx.observers.TestSubscriber

/**
 * Created by Karel Maesen, Geovise BVBA on 17/11/15.
 */
class RetryStrategyTest extends FlatSpec with Matchers with WithWireMock {

  def fileSource = "modules/client-java/src/test/resources/retry-scenario"



  val client = mkClientAcceptingJson

  behavior of "AtomiumClient (Scala) "

  it should "retry on transient errors with function converted to retrystrategy" in {

    import ImplicitConversions._

    val observable = client.feed("/feeds/events", classOf[Event])
      .withRetry( (cnt: Int, t: Throwable) => if (cnt > 2) throw t else 1000l )
      .observeFromBeginning(1000)
      .take(25)

    val testSubscriber: TestSubscriber[FeedEntry[Event]] = new TestSubscriber

    import rx.lang.scala.JavaConversions._
    //back to java so we can use the TestSubscriber
    toJavaObservable(observable).subscribe(testSubscriber)

    testSubscriber.awaitTerminalEvent(60, TimeUnit.SECONDS)
    testSubscriber.assertNoErrors()

  }

  it should "retry on transient errors with explicit retry strategy" in {

    resetWireMock

    val strategy = new RetryStrategy{
      override def apply(count: Int, throwable: Throwable): Long = if (count > 2) throw throwable else 1000l
    }

    val observable = client.feed("/feeds/events", classOf[Event])
      .withRetry( strategy )
      .observeFromBeginning(1000)
      .take(25)

    val testSubscriber: TestSubscriber[FeedEntry[Event]] = new TestSubscriber

    import rx.lang.scala.JavaConversions._
    //back to java so we can use the TestSubscriber
    toJavaObservable(observable).subscribe(testSubscriber)

    testSubscriber.awaitTerminalEvent(60, TimeUnit.SECONDS)
    testSubscriber.assertNoErrors()

  }


}
