package be.vlaanderen.awv.atom

import be.vlaanderen.awv.atom.jformat._
import org.joda.time.DateTime
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatterBuilder, DateTimeFormatter}
import scala.collection.JavaConverters._


package object format {

  implicit def feed2JFeed[T <: FeedContent](feed: Feed[T]): JFeed[T] = {
    val jfeed = new JFeed[T]
    jfeed.setBase(feed.base.path)
    jfeed.setId(feed.id)
    jfeed.setTitle(feed.title.getOrElse(null))
    jfeed.setGenerator(feed.generator.map(g => generator2JGenerator(g)).getOrElse(null))
    jfeed.setUpdated(feed.updated)
    jfeed.setLinks(feed.links.map(l => new JLink(l.rel, l.href.path)).asJava)
    jfeed.setEntries(feed.entries.map(e => entry2JEntry(e)).asJava)
    jfeed
  }

  implicit def generator2JGenerator(generator: Generator): JGenerator = {
    new JGenerator(generator.text, generator.uri.map(u => u.path).getOrElse(null),
      generator.version.getOrElse(null.asInstanceOf[String]))
  }

  implicit def entry2JEntry[T <: FeedContent](entry: Entry[T]): JEntry[T] = {
    new JEntry[T](new JContent[T](entry.content.value, entry.content.`type`),
      entry.links.map(l => new JLink(l.rel, l.href.path)).asJava)
  }

  implicit def jFeed2Feed[T <: FeedContent](jfeed: JFeed[T]): Feed[T] = {
    Feed(Url(jfeed.getBase), jfeed.getId, Option(jfeed.getTitle), jfeed.getGenerator, jfeed.getUpdated,
      jfeed.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList,
      jfeed.getEntries.asScala.map(e => jEntry2Entry(e)).toList)
  }

  implicit def jGenerator2Generator(jGenerator: JGenerator): Option[Generator] = {
    jGenerator match {
      case null => None
      case _ => Some(Generator(jGenerator.getText, Option(jGenerator.getUri).map(p => Url(p)),
        Option(jGenerator.getVersion)))
    }
  }

  implicit def jEntry2Entry[T <: FeedContent](jEntry: JEntry[T]): Entry[T] = {
    Entry[T](Content[T](jEntry.getContent.getValue, jEntry.getContent.getType),
      jEntry.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList)
  }


  val outputFormatterWithSecondsAndOptionalTZ: DateTimeFormatter = new DateTimeFormatterBuilder()
    .append(ISODateTimeFormat.dateHourMinuteSecond)
    .appendTimeZoneOffset("Z", true, 2, 4)
    .toFormatter


  def randomUuidUri = s"urn:uuid:${java.util.UUID.randomUUID().toString}"

  /**
   * The actual type of the Content in the feed MUST be either a String, a Class annotated with @XmlRootElement or
   * a JAXBElement wrapped object
   * but this is too difficult to represent in the scala's type system. A union type should be possible,
   * but a type that represents annotated classes?
   */
  type FeedContent = AnyRef

} 