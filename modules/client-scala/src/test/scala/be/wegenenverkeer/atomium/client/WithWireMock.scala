package be.wegenenverkeer.atomium.client

import be.wegenenverkeer.atomium.japi
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
 * Created by Karel Maesen, Geovise BVBA on 17/11/15.
 */
trait WithWireMock extends BeforeAndAfterAll {

  self: Suite =>

  import be.wegenenverkeer.atomium.client.ImplicitConversions._

  def fileSource: String

  def mappings = new SingleRootFileSource(fileSource)

  //we take a different port then in java-client module, because tests unfortunately continue to overlap with java client module
  val port: Int = 8089

  lazy val server = new WireMockServer(wireMockConfig.port(port).fileSource(mappings))


  def mkClientAcceptingXml = new japi.client.AtomiumClient.Builder()
    .setBaseUrl(s"http://localhost:$port/")
    .setAcceptXml()
    .build
    .asScala

  def mkClientAcceptingJson = new japi.client.AtomiumClient.Builder()
    .setBaseUrl(s"http://localhost:$port/")
    .setAcceptJson()
    .build
    .asScala


  override def beforeAll {
    server.start()
    configureFor("localhost", port)
    WireMock.resetToDefault
  }

  override def afterAll {
    server.shutdown()
    Thread.sleep(1000)
  }

}
