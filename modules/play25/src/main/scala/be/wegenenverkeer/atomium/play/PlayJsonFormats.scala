package be.wegenenverkeer.atomium.play

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import be.wegenenverkeer.atomium.api.FeedPage
import be.wegenenverkeer.atomium.format._
import be.wegenenverkeer.atomium.format.pub._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.JavaConverters._

/**
 * This object provides the Play JSON formats that can be used by read/write the Atom AST from/to JSON.
 *
 * If you want to use this implementation you will need to add a dependency on the Play Json API library.
 */
object PlayJsonFormats {

  implicit def offsetDateTimeFormatter(formatter: DateTimeFormatter): Writes.TemporalFormatter[OffsetDateTime] = new Writes.TemporalFormatter[OffsetDateTime] {
    def format(temporal: OffsetDateTime): String = formatter.format(temporal)
  }

  implicit val dateTimeFormat =
    Format[OffsetDateTime](
      Reads.DefaultZonedDateTimeReads.map( _.toOffsetDateTime),
      Writes.temporalWrites[OffsetDateTime, DateTimeFormatter](
        TimestampFormat.WRITE_FORMAT)
    )

  implicit val urlFormat = new Format[Url] {
    override def writes(url: Url): JsValue = JsString(url.getPath)

    override def reads(json: JsValue): JsResult[Url] = json match {
      case JsString(value) => JsSuccess(new Url(value))
      case _               => JsError(s"Can't read url value from $json")
    }
  }

  implicit val linkReads : Reads[Link] =
      ((__ \ "rel").read[String] and
        (__ \ "href").read[String]
        )( (rel, href) => new Link(rel, href))

  implicit val linkWrites : Writes[Link] =
    ((__ \ "rel").write[String] and
      (__ \ "href").write[String]
      )(in => (in.getRel, in.getHref))

  implicit val generatorReads : Reads[Generator]  =
    ( (__ \ "text").read[String] and ( __ \ "uri").read[String] and ( __ \ "version").read[String])(
      (text, uri, version) => new Generator(text, uri, version) )

  implicit val generatorWrites : Writes[Generator] =
    (( __ \ "text").write[String] and
      ( __ \ "uri").write[String] and
      ( __ \ "version").write[String]
      )( (in ) => (in.getText, in.getUri, in.getVersion))

  implicit val draftFormat = new Format[Draft] {
    override def reads(json: JsValue): JsResult[Draft] = json match {
      case JsString(value) if value == Draft.YES.getValue => JsSuccess(Draft.YES)
      case JsString(value) if value == Draft.NO.getValue  => JsSuccess(Draft.NO)
      case _                                          => JsError(s"Can't read Draft from $json")
    }

    override def writes(o: Draft): JsValue = JsString(o.getValue)
  }

  implicit val controlReads : Reads[Control] =
    ( __ \ "draft").readNullable[Draft].map {
      case Some(d) => new Control(d)
      case _ => new Control(null)
    }

  implicit val controlWrites : Writes[Control] = new Writes[Control]{
    override def writes(o: Control): JsValue = if (o.getDraft ==null) {
      Json.obj("draft" -> JsNull)
    } else Json.obj("draft" -> o.getDraft)
  }

  implicit def contentWrites[T: Writes]: Writes[Content[T]] = (
    (__ \ "value").write[T] and
      (__ \ "type").write[String]
    )(in => (in.getValue, in.getType))

  implicit def contentReads[T: Reads]: Reads[Content[T]] = (
    (__ \ "value").read[T] and
      (__ \ "type").readNullable[String]
    )((value, `type`) => new Content[T](value, `type`.getOrElse("")))

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
      (__ \ "updated").write[OffsetDateTime] and
      (__ \ "content").write[Content[T]] and
      (__ \ "links").write[List[Link]] and
      (__ \ "_type").write[String] // type information
    )(in => (in.getId, in.getUpdated, in.getContent, in.getLinks.asScala.toList, "atom"))

  implicit def atomPubEntryWrites[T: Writes]: Writes[AtomPubEntry[T]] = (
    (__ \ "id").write[String] and
      (__ \ "updated").write[OffsetDateTime] and
      (__ \ "content").write[Content[T]] and
      (__ \ "links").write[List[Link]] and
      (__ \ "edited").write[OffsetDateTime] and
      (__ \ "control").write[Control] and
      (__ \ "_type").write[String] // type information
    )(in => (in.getId, in.getUpdated, in.getContent, in.getLinks.asScala.toList, in.getEdited, in.getControl, "atom-pub"))

  implicit def entryReads[T: Reads]: Reads[Entry[T]] = new Reads[Entry[T]] {
    override def reads(json: JsValue): JsResult[Entry[T]] = {
      val hasControl = (json \ "_type").asOpt[String]
      hasControl match {
        case Some("atom-pub") => atomPubEntryReads[T].reads(json)
        case _                => atomEntryReads[T].reads(json)
      }
    }
  }

  implicit def atomEntryReads[T: Reads]: Reads[AtomEntry[T]] = (
    (__ \ "id").read[String] and
      (__ \ "updated").read[OffsetDateTime] and
      (__ \ "content").read[Content[T]] and
      (__ \ "links").readNullable[List[Link]]
    )((id, updated, content, links) => new AtomEntry[T](id, updated, content, links.getOrElse(Nil).asJava))

  implicit def atomPubEntryReads[T: Reads]: Reads[AtomPubEntry[T]] = (
    (__ \ "id").read[String] and
      (__ \ "updated").read[OffsetDateTime] and
      (__ \ "content").read[Content[T]] and
      (__ \ "links").readNullable[List[Link]] and
      (__ \ "edited").read[OffsetDateTime] and
      (__ \ "control").read[Control]
    )((id, updated, content, links, edited, control) => new AtomPubEntry[T](id, updated, content, links.getOrElse(Nil).asJava, edited, control))

  // candidate for macro format
  implicit def feedWrites[T: Writes]: Writes[FeedPage[T]] = (
    (__ \ "id").write[String] and
      (__ \ "base").write[Url] and
      (__ \ "title").writeNullable[String] and
      (__ \ "generator").writeNullable[Generator] and
      (__ \ "updated").write[OffsetDateTime] and
      (__ \ "links").write[List[Link]] and
      (__ \ "entries").write[List[Entry[T]]]
    )(in => (in.getId, new Url(in.getBase), Option(in.getTitle), Option(in.getGenerator), in.getUpdated,
                  in.getLinks.asScala.toList, in.getEntries.asScala.toList))

  implicit def feedReads[T: Reads]: Reads[FeedPage[T]] = (
    (__ \ "id").read[String] and
      (__ \ "base").read[Url] and
      (__ \ "title").readNullable[String] and
      (__ \ "generator").readNullable[Generator] and
      (__ \ "updated").read[OffsetDateTime] and
      (__ \ "links").read[List[Link]] and
      (__ \ "entries").read[List[Entry[T]]]
    )((id, base, title, generator, updated, links, entries) => new FeedPage[T](id, base.getPath, title.getOrElse(""), generator.orNull, updated, links.asJava, entries.asJava))

}
