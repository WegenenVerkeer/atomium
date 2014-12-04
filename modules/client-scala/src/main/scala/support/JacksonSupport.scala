package support

import be.wegenenverkeer.atom.Marshallers
import Marshallers.{JsonMarshaller, JsonUnmarshaller}
import com.fasterxml.jackson.databind.{ObjectReader, ObjectWriter}
import play.api.mvc.Codec

object JacksonSupport {

  def toJsonBytes(obj: AnyRef)(implicit codec: Codec, writer: ObjectWriter): Array[Byte] =
    writer.writeValueAsString(obj).getBytes(codec.charset)

  def jacksonMarshaller[T <:AnyRef](implicit codec: Codec, writer: ObjectWriter): JsonMarshaller[T] = {
    t:T => toJsonBytes(t)
  }

  def fromJsonBytes[T <:AnyRef](bytes: Array[Byte], charset: String)(implicit reader: ObjectReader): T = {
    reader.readValue(new String(bytes, charset))
  }

  def fromJsonString[T](json: String)(implicit reader: ObjectReader): T = {
    reader.readValue(json)
  }

  def jacksonUnmarshaller[T <:AnyRef](implicit reader: ObjectReader): JsonUnmarshaller[T] = {
    json: String => fromJsonString(json).asInstanceOf[T]
  }

}
