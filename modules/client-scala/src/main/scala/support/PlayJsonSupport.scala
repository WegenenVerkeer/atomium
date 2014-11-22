package support

import be.vlaanderen.awv.atom.Marshallers._
import play.api.libs.json.{Writes, Json, Reads}

object PlayJsonSupport {

  def jsonUnmarshaller[T <:AnyRef](implicit fmt: Reads[T]): JsonUnmarshaller[T] = {
    json => Json.parse(json).as[T]
  }

  def jsonMarshaller[T](implicit writes: Writes[T]): JsonMarshaller[T] = {
    t => Json.toJson(t).toString().getBytes("utf-8")
  }
}
