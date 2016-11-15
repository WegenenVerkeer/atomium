package be.wegenenverkeer.atomium.server.play

import be.wegenenverkeer.atomium.format.FeedPage
import be.wegenenverkeer.atomium.play.PlayJsonSupport
import play.api.http.MimeTypes
import play.api.libs.json.Writes
import be.wegenenverkeer.atomium.play.PlayJsonFormats.feedWrites

/**
 * A Feed marshaller backed by a `play json` marshaller
 *
 * @param contentType - the desired content type of the serialized `Feed`
 * @param writes - a implicitly provided play-json [[Writes]] for `T`
 * @tparam T - the type of the `Feed` content
 */
case class PlayJsonFeedMarshaller[T](contentType: String = MimeTypes.JSON)(implicit writes: Writes[T]) extends FeedMarshaller[T] {

  /** Serializes a `Feed` to JSON format. */
  override def marshall(feed: FeedPage[T]): (ContentType, Array[Byte]) = {
    (contentType, PlayJsonSupport.jsonMarshaller(feedWrites(writes))(feed))
  }
}
