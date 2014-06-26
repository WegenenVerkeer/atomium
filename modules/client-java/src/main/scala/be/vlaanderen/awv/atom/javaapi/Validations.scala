package be.vlaanderen.awv.atom.javaapi

import scalaz._

object Validations {

  def toException(validation:ValidationNel[String, Unit]) : Unit  = {
    validation match  {
      case Failure(errMsg) =>
        val message = errMsg.list.mkString("[", "] :: [", "]")
        throw new FeedProcessingException(message)
      case _ => ()
    }
  }
}
