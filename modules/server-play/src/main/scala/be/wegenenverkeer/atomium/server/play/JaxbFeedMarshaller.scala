package be.wegenenverkeer.atomium.server.play

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atomium.format.Feed
import be.wegenenverkeer.atomium.play.JaxbSupport
import play.api.http.MimeTypes

/**
 * A Feed marshaller backed by a `Jaxb` marshaller
 *
 * @param contentType - the desired content type of the serialized `Feed`
 * @param jaxbContext - a implicitly provided [[JAXBContext]]
 * @tparam T - the type of the `Feed` content
 */
case class JaxbFeedMarshaller[T](contentType: String = MimeTypes.XML)(implicit jaxbContext: JAXBContext) extends FeedMarshaller[T] {

  /** Serializes a `Feed` to XML format */
  override def marshall(feed: Feed[T]): (ContentType, Array[Byte]) = {
    val xmlMarshaller : Feed[T] => Array[Byte]= JaxbSupport.jaxbMarshaller
    (contentType, xmlMarshaller(feed))
  }
}