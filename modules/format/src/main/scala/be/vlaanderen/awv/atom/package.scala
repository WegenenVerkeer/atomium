package be.vlaanderen.awv

import play.api.libs.json._
import play.api.libs.functional.syntax._

package object atom {

  implicit val urlFormat = new Format[Url] {
    override def writes(url: Url): JsValue = JsString(url.path)
    override def reads(json: JsValue): JsResult[Url] = json match {
      case JsString(value) => JsSuccess(Url(value))
      case _ => JsError(s"Can't read url value from $json")
    }
  } 
    
  implicit val linkFormat = Json.format[Link]

  implicit def contentWrites[T](implicit fmt: Writes[T]): Writes[Content[T]] = (
    (__ \ "value").write[List[T]] and
    (__ \ "rawType").write[String]
  )(in => (in.value, in.rawType))

  implicit def contentReads[T](implicit fmt: Reads[T]): Reads[Content[T]] = (
    (__ \ "value").read[List[T]] and
    (__ \ "rawType").read[String]
  )((value, rawType) => Content[T](value, rawType))


  implicit def entryWrites[T](implicit fmt: Writes[T]): Writes[Entry[T]] = (
    (__ \ "content").write[Content[T]] and
    (__ \ "links").write[List[Link]]
  )(in => (in.content, in.links))

  implicit def entryReads[T](implicit fmt: Reads[T]): Reads[Entry[T]] = (
    (__ \ "content").read[Content[T]] and
    (__ \ "links").read[List[Link]]
  )((content, links) => Entry[T](content, links))


  // candidate for macro format
  implicit def feedWrites[T](implicit fmt: Writes[T]): Writes[Feed[T]] = (
    (__ \ "id").write[String] and
    (__ \ "base").write[Url] and
    (__ \ "title").writeNullable[String] and
    (__ \ "updated").write[String] and
    (__ \ "links").write[List[Link]] and
    (__ \ "entries").write[List[Entry[T]]]
  )(in => (in.id, in.base, in.title, in.updated, in.links, in.entries))

  implicit def feedReads[T](implicit fmt: Reads[T]): Reads[Feed[T]] = (
    (__ \ "id").read[String] and
    (__ \ "base").read[Url] and
    (__ \ "title").readNullable[String] and
    (__ \ "updated").read[String] and
    (__ \ "links").read[List[Link]] and
    (__ \ "entries").read[List[Entry[T]]]
  )((id, base, title, updated, links, entries) => Feed[T](id, base, title, updated, links, entries))



} 