package be.vlaanderen.awv

import be.vlaanderen.awv.atom.format.{FeedContent, Feed}

import scala.util.Try

package object atom {

  type FeedProcessingResult = Try[Unit]

  type FeedEntryUnmarshaller[T <: FeedContent] = (String) => Try[Feed[T]]
}
