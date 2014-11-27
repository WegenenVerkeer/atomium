package be.vlaanderen.awv

import be.vlaanderen.awv.atom.Marshallers.{JsonUnmarshaller, XmlUnmarshaller}

import scala.util.{Failure, Success, Try}

package object atom {

  type FeedProcessingResult = Try[Unit]

  type FeedEntryUnmarshaller[T] = (String) => Try[Feed[T]]

  trait FeedUnmarshaller[T] {

    def jsonUnmarshaller: JsonUnmarshaller[Feed[T]]
    def xmlUnmarshaller: XmlUnmarshaller[Feed[T]]

    def unmarshal(contentType: Option[String], body: String): Try[Feed[T]] = {
      try {
        contentType match {
          case Some(contentType) if contentType.startsWith("application/json") => Success(jsonUnmarshaller(body))
          case _ => Success(xmlUnmarshaller(body))
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          //TODO log it
          Failure(new FeedProcessingException(None, s"problem unmarshalling feed : ${e.getMessage}"))
      }
    }

  }

}
