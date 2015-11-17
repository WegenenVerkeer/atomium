package be.wegenenverkeer.atomium.client

import java.util.concurrent.TimeUnit

import be.wegenenverkeer.atomium.japi.client.FeedEntry
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import rx.lang.scala.Observable
import rx.observers.TestSubscriber

/**
 * Created by Karel Maesen, Geovise BVBA on 17/11/15.
 */
class RetryStrategyTest extends FlatSpec with Matchers with WithWireMock {

  def fileSource = "modules/client-java/src/test/resources/retry-scenario"



  val client = mkClientAcceptingJson

  behavior of "AtomiumClient (Scala) "

  it should "retry on transient errors" in {

    val observable = client.feed("/feeds/events", classOf[Event])
      .withRetryStrategy((cnt, t) => if (cnt > 2) throw t else 1000)
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
