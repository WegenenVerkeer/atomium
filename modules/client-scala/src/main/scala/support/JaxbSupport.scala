package support

import java.io._
import javax.xml.bind._

import be.wegenenverkeer.atom.Marshallers
import Marshallers._
import play.api.mvc._

object JaxbSupport {

  def toXmlBytes(obj: AnyRef)(implicit codec: Codec, context: JAXBContext): Array[Byte] = {
    val marshaller = context.createMarshaller()
    val stream = new ByteArrayOutputStream

    marshaller.setProperty(Marshaller.JAXB_ENCODING, codec.charset)
    marshaller.marshal(obj, stream)

    stream.toByteArray
  }

  def jaxbMarshaller[T <:AnyRef](implicit jaxbContext: JAXBContext): XmlMarshaller[T] = {
    t:T => toXmlBytes(t)
  }

  def fromXmlString[A](xml: String, expectedType: Class[A])(implicit context: JAXBContext): A = {
    val result = context.createUnmarshaller().unmarshal(new StringReader(xml))
//    expectedType.cast(result)
    result.asInstanceOf[A]
  }

  def fromXmlBytes[A](bytes: Array[Byte], charset: String, expectedType: Class[A])(implicit context: JAXBContext): A = {
    val stream = new ByteArrayInputStream(bytes)
    val reader = new InputStreamReader(stream, charset)
    val result = context.createUnmarshaller().unmarshal(reader)
    expectedType.cast(result)
  }

  def jaxbUnmarshaller[T](implicit jaxbContext: JAXBContext, manifest: Manifest[T]): XmlUnmarshaller[T] = {
        //TODO check manifest !!!
    xml => fromXmlString(xml, manifest.runtimeClass).asInstanceOf[T]
  }

}

