package be.wegenenverkeer.atom

object Marshallers {

  type XmlMarshaller[T] = T => Array[Byte]
  type XmlUnmarshaller[T] = String => T

  type JsonMarshaller[T] = T => Array[Byte]
  type JsonUnmarshaller[T] = String => T

}
