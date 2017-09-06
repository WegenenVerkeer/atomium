package be.wegenenverkeer.atomium.play

import be.wegenenverkeer.atomium.api.Codec
import play.api.libs.json.{Format, Json}

/**
  * Codec for encoding Entries using Play Json
  * @param entryFormat the Format for the Entry
  * @tparam E The type of the Entry
  */
case class PlayJsonEntryCodec[E](implicit val entryFormat: Format[E]) extends Codec[E, String] {

  override def getMimeType: String = "application/json"

  override def encode(entry: E):String = Json.toJson(entry).toString

  override def decode(encoded: String): E = Json.parse(encoded).as[E]

}