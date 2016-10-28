package be.wegenenverkeer.atomium.server.play

import be.wegenenverkeer.atomium.format.Feed
import be.wegenenverkeer.atomium.play.JacksonSupport
import com.fasterxml.jackson.databind.ObjectWriter
import play.api.http.MimeTypes
import play.api.mvc.Codec


/**
 * A Feed marshaller backed by a `Jackson` marshaller
 *
 * @param contentType - the desired content type of the serialized `Feed`
 * @param codec - a implicitly provided [[Codec]]
 * @param writer - a implicitly provided [[ObjectWriter]]
 * @tparam T - the type of the `Feed` content
 */
case class JacksonFeedMarshaller[T](contentType: String = MimeTypes.JSON)(implicit codec: Codec, writer: ObjectWriter)
  extends FeedMarshaller[T] {

  /** Serializes a `Feed` to JSON format */
  override def marshall(feed: Feed[T]): (ContentType, Array[Byte]) = {
    val xmlMarshaller: Feed[T] => Array[Byte] =  JacksonSupport.jacksonMarshaller
    (contentType, xmlMarshaller(feed))
  }
}
