package be.wegenenverkeer.atomium.server.play

import javax.xml.bind.JAXBContext

import be.wegenenverkeer.atomium.format.Feed
import be.wegenenverkeer.atomium.format.FeedConverters._
import be.wegenenverkeer.atomium.play.{JacksonSupport, JaxbSupport, PlayJsonSupport}
import com.fasterxml.jackson.databind.ObjectWriter
import play.api.http.MimeTypes
import play.api.libs.json.Writes
import play.api.mvc.Codec

trait FeedMarshaller[T] {
  def marshaller(feed: Feed[T]): Array[Byte]
  def contentType: String
}

object FeedMarshaller {

  def playJson[T](_contentType: String)(implicit writes: Writes[Feed[T]]): FeedMarshaller[T] = {

    new FeedMarshaller[T] {
      override def marshaller(feed: Feed[T]): Array[Byte] = {
        PlayJsonSupport.jsonMarshaller(writes)(feed)
      }

      override def contentType: String = _contentType
    }

  }

  def playJson[T](implicit writes: Writes[Feed[T]]): FeedMarshaller[T] = playJson(MimeTypes.JSON)



  def jaxbMarshaller[T](_contentType: String)(implicit jaxbContext: JAXBContext): FeedMarshaller[T] = {

    new FeedMarshaller[T] {
      override def contentType: String = _contentType

      override def marshaller(feed: Feed[T]): Array[Byte] = {
        val toJavaEventFeed = (feed: Feed[T]) => feed.asJava
        val xmlMarshaller = toJavaEventFeed andThen JaxbSupport.jaxbMarshaller
        xmlMarshaller(feed)
      }
    }

  }

  def jaxbMarshaller[T](implicit jaxbContext: JAXBContext): FeedMarshaller[T] = jaxbMarshaller(MimeTypes.XML)



  def jacksonMarshaller[T](contentType: String)(implicit codec: Codec, writer: ObjectWriter): FeedMarshaller[T] = {

    new FeedMarshaller[T] {
      override def contentType: String = contentType

      override def marshaller(feed: Feed[T]): Array[Byte] = {
        val toJavaEventFeed = (feed: Feed[T]) => feed.asJava
        val xmlMarshaller = toJavaEventFeed andThen JacksonSupport.jacksonMarshaller
        xmlMarshaller(feed)
      }
    }

  }

  def jacksonMarshaller[T](implicit codec: Codec, writer: ObjectWriter): FeedMarshaller[T] = jacksonMarshaller(MimeTypes.XML)
}