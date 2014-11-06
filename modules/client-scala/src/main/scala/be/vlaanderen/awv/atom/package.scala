package be.vlaanderen.awv

import scala.util.Try

package object atom {

  type FeedProcessingResult = Try[Unit]

  type FeedEntryUnmarshaller[T] = (String) => Try[Feed[T]]
}
