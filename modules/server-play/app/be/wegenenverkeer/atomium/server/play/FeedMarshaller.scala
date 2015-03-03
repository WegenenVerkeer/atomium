package be.wegenenverkeer.atomium.server.play

import be.wegenenverkeer.atomium.format.Feed

trait FeedMarshaller[T] {

  /** The content type */
  type ContentType = String

  /**
   * Serializes a `Feed` to `Array[Byte]` format
   *
   * @return a tuple of `ContentType` (ie: String) indicating the serialized format and an `Array[Byte]` containing the serialized `Feed`.
   */
  def marshall(feed: Feed[T]): (ContentType, Array[Byte])

}
