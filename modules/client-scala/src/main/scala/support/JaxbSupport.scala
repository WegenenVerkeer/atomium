package support

import java.io._
import javax.xml.bind._

import com.sun.jersey.api.json.JSONJAXBContext
import play.api.http._
import play.api.mvc._

case class JaxbXml(obj: AnyRef)
case class JaxbJson(obj: AnyRef)
case class Jaxb(obj: AnyRef)

trait JaxbSupport extends JaxbWriteables

object JaxbSupport {

  def toXmlBytes(obj: AnyRef)(implicit codec: Codec, context: JAXBContext): Array[Byte] = {
    val marshaller = context.createMarshaller()
    val stream = new ByteArrayOutputStream

    marshaller.setProperty(Marshaller.JAXB_ENCODING, codec.charset)
    marshaller.marshal(obj, stream)

    stream.toByteArray
  }

  def toJsonBytes(obj: AnyRef)(implicit codec: Codec, context: JSONJAXBContext): Array[Byte] = {
    val marshaller = context.createJSONMarshaller()
    val stream = new ByteArrayOutputStream
    val writer = new OutputStreamWriter(stream, codec.charset)

    marshaller.marshallToJSON(obj, writer)

    writer.flush()
    stream.toByteArray
  }

  def fromXmlBytes[A](bytes: Array[Byte], charset: String, expectedType: Class[A])(implicit context: JAXBContext): A = {
    val stream = new ByteArrayInputStream(bytes)
    val reader = new InputStreamReader(stream, charset)
    val result = context.createUnmarshaller().unmarshal(reader)
    expectedType.cast(result)
  }

  def fromJsonBytes[A](bytes: Array[Byte], charset: String, expectedType: Class[A])(implicit context: JSONJAXBContext): A = {
    val stream = new ByteArrayInputStream(bytes)
    val reader = new InputStreamReader(stream, charset)
    val result = context.createJSONUnmarshaller().unmarshalFromJSON(reader, expectedType)
    expectedType.cast(result)
  }
}

trait JaxbWriteables {

  implicit def contentTypeOf_JaxbXml(implicit codec: Codec): ContentTypeOf[JaxbXml] = {
    ContentTypeOf[JaxbXml](Some(ContentTypes.XML))
  }

  implicit def contentTypeOf_JaxbJson(implicit codec: Codec): ContentTypeOf[JaxbJson] = {
    ContentTypeOf[JaxbJson](Some(ContentTypes.JSON))
  }

  implicit def contentTypeOf_Jaxb(implicit codec: Codec, header: RequestHeader): ContentTypeOf[Jaxb] = ContentTypeOf[Jaxb] {
    header.headers.get(HeaderNames.ACCEPT) match {
      case Some("application/json") => Some(ContentTypes.JSON)
      case _                        => Some(ContentTypes.XML)
    }
  }

  implicit def writeableOf_JaxbXml(implicit codec: Codec, context: JAXBContext): Writeable[JaxbXml] = {
    Writeable[JaxbXml]((xml: JaxbXml) => JaxbSupport.toXmlBytes(xml.obj))
  }

  implicit def writeableOf_JaxbJson(implicit codec: Codec, context: JSONJAXBContext): Writeable[JaxbJson] = {
    Writeable[JaxbJson]((json: JaxbJson) => JaxbSupport.toJsonBytes(json.obj))
  }

  implicit def writeableOf_JaxbNeg(implicit codec: Codec, context: JSONJAXBContext, header: RequestHeader): Writeable[Jaxb] = {
    Writeable[Jaxb] {
      (jaxb: Jaxb) => header.headers.get(HeaderNames.ACCEPT) match {
        case Some("application/json") => JaxbSupport.toJsonBytes(jaxb.obj)
        case _                        => JaxbSupport.toXmlBytes(jaxb.obj)
      }
    }
  }
}


