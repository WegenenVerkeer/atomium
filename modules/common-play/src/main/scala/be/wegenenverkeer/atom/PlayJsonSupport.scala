package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.Marshallers._
import play.api.libs.json.{Json, Reads, Writes}

object PlayJsonSupport {

  def jsonUnmarshaller[T <:AnyRef](implicit reads: Reads[T]): JsonUnmarshaller[T] = {
    json => Json.parse(json).as[T]
  }

  def jsonMarshaller[T](implicit writes: Writes[T]): JsonMarshaller[T] = {
    t => Json.toJson(t).toString().getBytes("utf-8")
  }
}
