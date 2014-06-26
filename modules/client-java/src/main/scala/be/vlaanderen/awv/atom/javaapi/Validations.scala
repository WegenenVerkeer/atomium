package be.vlaanderen.awv.atom.javaapi

import be.vlaanderen.awv.atom.{FeedPosition, FeedProcessingResult}

import scalaz._

object Validations {

  def valueOrException(validation:FeedProcessingResult) : FeedPosition  = {
    validation match  {
      case Failure(errMsg) =>
        val message = errMsg.list.mkString("[", "] :: [", "]")
        throw new FeedProcessingException(message, null)
      case Success(feedPos) => feedPos
    }
  }
}
