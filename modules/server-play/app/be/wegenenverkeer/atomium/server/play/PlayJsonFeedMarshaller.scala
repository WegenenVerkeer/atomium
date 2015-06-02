package be.wegenenverkeer.atomium.server.play

import be.wegenenverkeer.atomium.format.Feed
import be.wegenenverkeer.atomium.play.PlayJsonSupport
import play.api.http.MimeTypes
import play.api.libs.json.Writes

/**
 * A Feed marshaller backed by a `play json` marshaller
 *
 * @param contentType - the desired content type of the serialized `Feed`
 * @param writes - a implicitly provided play-json [[Writes]] for `Feed[T]`
 * @tparam T - the type of the `Feed` content
 */
case class PlayJsonFeedMarshaller[T](contentType: String = MimeTypes.JSON)(implicit writes: Writes[Feed[T]]) extends FeedMarshaller[T] {

  /** Serializes a `Feed` to JSON format. */
  override def marshall(feed: Feed[T]): (ContentType, Array[Byte]) = {
    (contentType, PlayJsonSupport.jsonMarshaller(writes)(feed))
  }
}
