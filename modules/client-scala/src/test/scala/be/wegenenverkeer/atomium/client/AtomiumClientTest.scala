package be.wegenenverkeer.atomium.client

import java.util.concurrent.TimeUnit
import javax.xml.bind.annotation._

import be.wegenenverkeer.atomium.japi
import be.wegenenverkeer.atomium.japi.client.FeedEntry
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import be.wegenenverkeer.atomium.client.ImplicitConversions._
import be.wegenenverkeer.atomium.format.Entry
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import rx.lang.scala._
import rx.observers.TestSubscriber

/**
 * Simple functional Spec for AtomiumClient
 *
 * Created by Karel Maesen, Geovise BVBA on 16/04/15.
 */
class AtomiumClientTest extends FlatSpec with Matchers  with WithWireMock {

  def fileSource = "modules/client-java/src/test/resources/basis-scenario"

  val client = mkClientAcceptingXml

  behavior of "AtomiumClient (Scala) "

  it should "receive all events since a specific event in an Observable" in {

    val observable: Observable[FeedEntry[Event]] = client.feed("/feeds/events", classOf[Event])
      .observeSince("urn:uuid:8641f2fd-e8dc-4756-acf2-3b708080ea3a", "20/forward/10", 1000)

    val testSubscriber: TestSubscriber[FeedEntry[Event]] = new TestSubscriber

    import rx.lang.scala.JavaConversions._ //back to java so we can use the TestSubscriber
    toJavaObservable(observable.take(100)).subscribe(testSubscriber)

    testSubscriber.awaitTerminalEvent(30, TimeUnit.SECONDS)
    testSubscriber.assertNoErrors()
  }


}

//@XmlRootElement
//@XmlAccessorType(XmlAccessType.NONE)
//class Event {
//
//  @XmlElement var value: Double = null.asInstanceOf[Double]
//  @XmlElement var description: String = null.asInstanceOf[String]
//  @XmlAttribute var version: Integer = null.asInstanceOf[Integer]
//
//  override def toString: String = {
//    "Event " + version + " " + "description " + " value: " + value
//  }
//}
