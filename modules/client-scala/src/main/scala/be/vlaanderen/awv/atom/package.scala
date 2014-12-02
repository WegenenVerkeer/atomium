package be.vlaanderen.awv

import be.vlaanderen.awv.atom.Marshallers.{JsonUnmarshaller, XmlUnmarshaller}

import scala.util.control.NonFatal
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
          case AnyJson(_) => Success(jsonUnmarshaller(body))
          case _ => Success(xmlUnmarshaller(body))
        }
      } catch {
        case NonFatal(e) =>
          Failure(new FeedProcessingException(None, s"problem unmarshalling feed : ${e.getMessage}"))
      }
    }

    object AnyJson {
      def unapply(contentTypeOpt: Some[String]): Option[String] =
        if (contentTypeOpt.get.startsWith("application/json"))
          contentTypeOpt
        else
          None
    }

  }

}
