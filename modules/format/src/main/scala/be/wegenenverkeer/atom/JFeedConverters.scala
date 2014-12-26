package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.java.{Feed => JFeed, Entry => JEntry, Generator => JGenerator, Content => JContent, Link => JLink}
import scala.collection.JavaConverters._

object JFeedConverters {

  implicit def feed2JFeed[T](feed: Feed[T]): JFeed[T] = {
    new JFeed[T](
      feed.id,
      feed.base.path,
      feed.title.orNull,
      feed.generator.map(generator2JGenerator).orNull,
      feed.updated,
      feed.links.map(l => new JLink(l.rel, l.href.path)).asJava,
      feed.entries.map(e => entry2JEntry(e)).asJava
    )
  }

  implicit def generator2JGenerator(generator: Generator): JGenerator = {
    new JGenerator(
      generator.text,
      generator.uri.map(u => u.path).orNull,
      generator.version.getOrElse(null.asInstanceOf[String])
    )
  }

  implicit def entry2JEntry[T](entry: Entry[T]): JEntry[T] = {
    new JEntry[T](
      entry.id,
      entry.updated,
      new JContent[T](entry.content.value, entry.content.`type`),
      entry.links.map(l => new JLink(l.rel, l.href.path)).asJava
    )
  }

  implicit def jFeed2Feed[T](jfeed: be.wegenenverkeer.atom.java.Feed[T]): Feed[T] = {
    Feed(
      jfeed.getId,
      Url(jfeed.getBase),
      Option(jfeed.getTitle),
      jfeed.getGenerator,
      jfeed.getUpdated,
      jfeed.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList,
      jfeed.getEntries.asScala.map(e => jEntry2Entry(e)).toList
    )
  }

  implicit def jGenerator2Generator(jGenerator: JGenerator): Option[Generator] = {
    jGenerator match {
      case null => None
      case _ => Some(Generator(jGenerator.getText, Option(jGenerator.getUri).map(p => Url(p)),
        Option(jGenerator.getVersion)))
    }
  }

  implicit def jEntry2Entry[T](jEntry: JEntry[T]): Entry[T] = {
    Entry[T](
      jEntry.getId,
      jEntry.getUpdated,
      Content[T](jEntry.getContent.getValue, jEntry.getContent.getType),
      jEntry.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList
    )
  }
}
