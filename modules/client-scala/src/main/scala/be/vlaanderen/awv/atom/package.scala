package be.vlaanderen.awv

import scalaz._

package object atom {

  type FeedProcessingResult = Validation[String, FeedPosition]

}
