package be.wegenenverkeer.atomium.play

import be.wegenenverkeer.atomium.api.{FeedPage, FeedPageCodec}
import play.api.libs.json.{Format, Json}

/**
  * Created by Karel Maesen, Geovise BVBA on 18/11/16.
  */
case class PlayJsonCodec[E](implicit val entryFormat: Format[E]) extends FeedPageCodec[E, Array[Byte]] {

  import PlayJsonFormats._

  override def getMimeType: String = "application/json"

  override def encode(page: FeedPage[E]): Array[Byte] =
    Json.toJson(page).toString.getBytes("UTF-8")

  override def decode(encoded: Array[Byte]): FeedPage[E] = ??? // we don't yet need this

}
