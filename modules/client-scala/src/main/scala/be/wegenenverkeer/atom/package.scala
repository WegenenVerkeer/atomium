package be.wegenenverkeer

import be.wegenenverkeer.atomium.format.Feed

import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

package object atom {

  type FeedEntryUnmarshaller[E] = (String) => Try[Feed[E]]

  trait FeedUnmarshaller[E] {

    private var unmarshallerRegistry: Map[String, String => Feed[E]] = Map.empty

    def registerUnmarshaller(mimeType: String, unmarshaller: String => Feed[E]): Unit = {
      unmarshallerRegistry += mimeType -> unmarshaller
    }

    def unmarshal(contentType: Option[String], body: String): Try[Feed[E]] = {
      try {
        contentType match {
          case AnyJson(_) if unmarshallerRegistry.contains("application/json")
            => Success(unmarshallerRegistry.get("application/json").get(body))
          case AnyXml(_)  if unmarshallerRegistry.contains("application/xml")
            => Success(unmarshallerRegistry.get("application/json").get(body))
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

    object AnyXml {
      def unapply(contentTypeOpt: Some[String]): Option[String] =
        if (contentTypeOpt.get.startsWith("application/xml"))
          contentTypeOpt
        else
          None
    }
  }

}
