package be.vlaanderen.awv.atom

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * This object provides the Play JSON formats that can be used by read/write the Atom AST from/to JSON.
 *
 * If you want to use this implementation you will need to add a dependency on the Play Json API library.
 */
object Formats {

  implicit val urlFormat = new Format[Url] {
    override def writes(url: Url): JsValue = JsString(url.path)
    override def reads(json: JsValue): JsResult[Url] = json match {
      case JsString(value) => JsSuccess(Url(value))
      case _ => JsError(s"Can't read url value from $json")
    }
  }

  implicit val linkFormat = Json.format[Link]

  implicit def contentWrites[T](implicit fmt: Writes[T]): Writes[Content[T]] = (
    (__ \ "value").write[T] and
      (__ \ "type").write[String]
    )(in => (in.value, in.`type`))

  implicit def contentReads[T](implicit fmt: Reads[T]): Reads[Content[T]] = (
    (__ \ "value").read[T] and
      (__ \ "type").read[String]
    )((value, `type`) => Content[T](value, `type`))

  implicit def generatorWrites: Writes[Generator] = (
    (__ \ "text").write[String] and
      (__ \ "uri").writeNullable[Url] and
      (__ \ "version").writeNullable[String]
    )(in => (in.text, in.uri, in.version))

  implicit def generatorReads: Reads[Generator] = (
    (__ \ "text").read[String] and
      (__ \ "uri").readNullable[Url] and
      (__ \ "version").readNullable[String]
    )((text, uri, version) => Generator(text, uri, version))

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
      (__ \ "generator").writeNullable[Generator] and
      (__ \ "updated").write[DateTime] and
      (__ \ "links").write[List[Link]] and
      (__ \ "entries").write[List[Entry[T]]]
    )(in => (in.id, in.base, in.title, in.generator, in.updated,in.links, in.entries))

  implicit def feedReads[T](implicit fmt: Reads[T]): Reads[Feed[T]] = (
    (__ \ "id").read[String] and
      (__ \ "base").read[Url] and
      (__ \ "title").readNullable[String] and
      (__ \ "generator").readNullable[Generator] and
      (__ \ "updated").read[DateTime] and
      (__ \ "links").read[List[Link]] and
      (__ \ "entries").read[List[Entry[T]]]
    )((id, base, title, generator, updated, links, entries) => Feed[T](id, base, title, generator, updated, links, entries))
}
