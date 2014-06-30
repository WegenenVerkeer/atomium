package be.vlaanderen.awv

import scalaz._

package object atom {

  type FeedProcessingResult = Validation[FeedProcessingError, FeedPosition]

  type FeedEntryUnmarshaller[T] = (String) => Validation[FeedProcessingError, Feed[T]]
}
