package be.wegenenverkeer.atomium.client

import be.wegenenverkeer.atomium.japi.client.{AtomiumClient => JAtomiumClient}
import be.wegenenverkeer.atomium.japi.client.AtomiumClient.{FeedObservableBuilder => JFeedObservableBuilder}
import rx.lang.scala.JavaConversions._


/**
 * Created by Karel Maesen, Geovise BVBA on 16/04/15.
 */

class AtomiumClient(val inner: JAtomiumClient) {
  def feed[E](path: String, entryTypeMarker: Class[E]) = new FeedObservableBuilder(inner.feed[E](path, entryTypeMarker))
  class Builder extends JAtomiumClient.Builder
}

class FeedObservableBuilder[E](val inner: JFeedObservableBuilder[E]) {

  def observe(intervalInMs: Int) = toScalaObservable(inner.observe(intervalInMs))

  def observeSince(entryId: String, pageUrl: String, intervalInMs: Int) =
    toScalaObservable(inner.observeSince(entryId, pageUrl, intervalInMs))
}


object ImplicitConversions {

  import rx.lang.scala.JavaConversions._



  trait JavaAtomiumClientWrapper {
    val inner: JAtomiumClient
    def asScala: AtomiumClient = new AtomiumClient(inner)
  }

  implicit def wrap(javaClient: JAtomiumClient) : JavaAtomiumClientWrapper = new JavaAtomiumClientWrapper{
    val inner = javaClient
  }

  implicit def unwrap(client: AtomiumClient) : JAtomiumClient = client.inner


}
