package be.wegenenverkeer.atomium.play

import play.api.libs.json.{Json, Reads, Writes}

object PlayJsonSupport {

  type JsonMarshaller[T] = T => Array[Byte]
  type JsonUnmarshaller[T] = String => T

  def jsonUnmarshaller[T <:AnyRef](implicit reads: Reads[T]): JsonUnmarshaller[T] = {
    json => Json.parse(json).as[T]
  }

  def jsonMarshaller[T](implicit writes: Writes[T]): JsonMarshaller[T] = {
    t => Json.toJson(t).toString().getBytes("utf-8")
  }
}
