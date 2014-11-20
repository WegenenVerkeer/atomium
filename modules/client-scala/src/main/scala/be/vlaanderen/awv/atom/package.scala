package be.vlaanderen.awv

import be.vlaanderen.awv.atom.Feed

import scala.util.Try

package object atom {

  type FeedProcessingResult = Try[Unit]

  type FeedEntryUnmarshaller[T] = (String) => Try[Feed[T]]
}
