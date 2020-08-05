package be.wegenenverkeer.atomium.play

import be.wegenenverkeer.atomium.api.{ FeedPage, FeedPageCodec }
import be.wegenenverkeer.atomium.format.JaxbCodec

/**
 * Created by Karel Maesen, Geovise BVBA on 18/11/16.
 */
case class PlayJaxbCodec[E](typeMarker: Class[E]) extends FeedPageCodec[E, Array[Byte]] {

  val delegate = new JaxbCodec(typeMarker)

  override def getMimeType: String = delegate.getMimeType

  override def encode(page: FeedPage[E]): Array[Byte] = delegate.encode(page).getBytes("UTF-8")

  override def decode(encoded: Array[Byte]): FeedPage[E] = delegate.decode(new String(encoded, "UTF-8"))
}
