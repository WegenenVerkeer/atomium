package be.wegenenverkeer.atomium.server.play

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atomium.format.FeedPage
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
  override def marshall(feed: FeedPage[T]): (ContentType, Array[Byte]) = {
    val xmlMarshaller : FeedPage[T] => Array[Byte]= JaxbSupport.jaxbMarshaller
    (contentType, xmlMarshaller(feed))
  }
}