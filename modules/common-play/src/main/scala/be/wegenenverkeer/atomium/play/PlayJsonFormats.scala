package be.wegenenverkeer.atomium.play

import be.wegenenverkeer.atomium.format._
import be.wegenenverkeer.atomium.format.pub._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * This object provides the Play JSON formats that can be used by read/write the Atom AST from/to JSON.
 *
 * If you want to use this implementation you will need to add a dependency on the Play Json API library.
 */
object PlayJsonFormats {

  val datePattern = "yyyy-MM-dd'T'HH:mm:ssZ"

  implicit val jodaDateTimeFormat =
    Format[DateTime](Reads.jodaDateReads(datePattern), Writes.jodaDateWrites(datePattern))

  implicit val urlFormat = new Format[Url] {
    override def writes(url: Url): JsValue = JsString(url.path)

    override def reads(json: JsValue): JsResult[Url] = json match {
      case JsString(value) => JsSuccess(Url(value))
      case _               => JsError(s"Can't read url value from $json")
    }
  }

  implicit val linkFormat = Json.format[Link]
  implicit val generatorFormat = Json.format[Generator]
  implicit val draftFormat = new Format[Draft] {
    override def reads(json: JsValue): JsResult[Draft] = json match {
      case JsString(value) if value == DraftYes.value => JsSuccess(DraftYes)
      case JsString(value) if value == DraftNo.value  => JsSuccess(DraftNo)
      case _                                          => JsError(s"Can't read Draft from $json")
    }

    override def writes(o: Draft): JsValue = JsString(o.value)
  }
  implicit val controlFormat = Json.format[Control]

  implicit def contentWrites[T: Writes]: Writes[Content[T]] = (
    (__ \ "value").write[T] and
      (__ \ "type").write[String]
    )(in => (in.value, in.`type`))

  implicit def contentReads[T: Reads]: Reads[Content[T]] = (
    (__ \ "value").read[T] and
      (__ \ "type").read[String]
    )((value, `type`) => Content[T](value, `type`))

  implicit def entryWrites[T: Writes]: Writes[Entry[T]] = new Writes[Entry[T]] {

    override def writes(o: Entry[T]): JsValue = {
      o match {
        case e: AtomPubEntry[T] => atomPubEntryWrites[T].writes(e)
        case e: AtomEntry[T]    => atomEntryWrites[T].writes(e)
      }
    }
  }

  implicit def atomEntryWrites[T: Writes]: Writes[AtomEntry[T]] = (
    (__ \ "id").write[String] and
      (__ \ "updated").write[DateTime] and
      (__ \ "content").write[Content[T]] and
      (__ \ "links").write[List[Link]] and
      (__ \ "_type").write[String] // type information
    )(in => (in.id, in.updated, in.content, in.links, "atom"))

  implicit def atomPubEntryWrites[T: Writes]: Writes[AtomPubEntry[T]] = (
    (__ \ "id").write[String] and
      (__ \ "updated").write[DateTime] and
      (__ \ "content").write[Content[T]] and
      (__ \ "links").write[List[Link]] and
      (__ \ "edited").write[DateTime] and
      (__ \ "control").write[Control] and
      (__ \ "_type").write[String] // type information
    )(in => (in.id, in.updated, in.content, in.links, in.edited, in.control, "atom-pub"))

  implicit def entryReads[T: Reads]: Reads[Entry[T]] = new Reads[Entry[T]] {
    override def reads(json: JsValue): JsResult[Entry[T]] = {
      val hasControl = (json \ "_type").as[String]
      hasControl match {
        case "atom-pub" => atomPubEntryReads[T].reads(json)
        case _          => atomEntryReads[T].reads(json)
      }
    }
  }

  implicit def atomEntryReads[T: Reads]: Reads[AtomEntry[T]] = (
    (__ \ "id").read[String] and
      (__ \ "updated").read[DateTime] and
      (__ \ "content").read[Content[T]] and
      (__ \ "links").read[List[Link]]
    )((id, updated, content, links) => AtomEntry[T](id, updated, content, links))

  implicit def atomPubEntryReads[T: Reads]: Reads[AtomPubEntry[T]] = (
    (__ \ "id").read[String] and
      (__ \ "updated").read[DateTime] and
      (__ \ "content").read[Content[T]] and
      (__ \ "links").read[List[Link]] and
      (__ \ "edited").read[DateTime] and
      (__ \ "control").read[Control]
    )((id, updated, content, links, edited, control) => AtomPubEntry[T](id, updated, content, links, edited, control))

  // candidate for macro format
  implicit def feedWrites[T: Writes]: Writes[Feed[T]] = (
    (__ \ "id").write[String] and
      (__ \ "base").write[Url] and
      (__ \ "title").writeNullable[String] and
      (__ \ "generator").writeNullable[Generator] and
      (__ \ "updated").write[DateTime] and
      (__ \ "links").write[List[Link]] and
      (__ \ "entries").write[List[Entry[T]]]
    )(in => (in.id, in.base, in.title, in.generator, in.updated, in.links, in.entries))

  implicit def feedReads[T: Reads]: Reads[Feed[T]] = (
    (__ \ "id").read[String] and
      (__ \ "base").read[Url] and
      (__ \ "title").readNullable[String] and
      (__ \ "generator").readNullable[Generator] and
      (__ \ "updated").read[DateTime] and
      (__ \ "links").read[List[Link]] and
      (__ \ "entries").read[List[Entry[T]]]
    )((id, base, title, generator, updated, links, entries) => Feed[T](id, base, title, generator, updated, links, entries))

}
