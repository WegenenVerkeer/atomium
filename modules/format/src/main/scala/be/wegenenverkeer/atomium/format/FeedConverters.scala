package be.wegenenverkeer.atomium.format

import be.wegenenverkeer.atomium.japi.format

import scala.collection.JavaConverters._


object FeedConverters {
  
  implicit class Feed2JFeed[T](feed: Feed[T]) {
    def asJava: format.Feed[T] = {
      new format.Feed[T](
        feed.id,
        feed.base.path,
        feed.title.orNull,
        feed.generator.map(_.asJava).orNull,
        feed.updated,
        feed.links.map(l => new format.Link(l.rel, l.href.path)).asJava,
        feed.entries.map(_.asJava).asJava
      )
    }
  }

  implicit class Generator2JGenerator(generator: Generator) {
    def asJava: format.Generator = {
      new format.Generator(
        generator.text,
        generator.uri.map(u => u.path).orNull,
        generator.version.orNull
      )
    }
  }

  implicit class Entry2JEntry[T](entry: Entry[T]) {
    def asJava:format.Entry[T] = {
      new format.Entry[T](
        entry.id,
        entry.updated,
        new format.Content[T](entry.content.value, entry.content.`type`),
        entry.links.map(l => new format.Link(l.rel, l.href.path)).asJava
      )
    }
  }

  implicit class JFeed2Feed[T](jfeed: format.Feed[T]) {
    def asScala: Feed[T] = {
      Feed(
        jfeed.getId,
        Url(jfeed.getBase),
        Option(jfeed.getTitle),
        jfeed.getGenerator.asScalaOpt,
        jfeed.getUpdated,
        jfeed.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList,
        jfeed.getEntries.asScala.map(_.asScala).toList
      )
    }
  }

  implicit class JGenerator2OptionGenerator(jGenerator: format.Generator){
    def asScalaOpt: Option[Generator] = {
      jGenerator match {
        case null => None
        case _    => Some(Generator(jGenerator.getText, Option(jGenerator.getUri).map(p => Url(p)), Option(jGenerator.getVersion)))
      }
    }
  }

  implicit class JEntry2Entry[T](jEntry: format.Entry[T]) {
    def asScala: Entry[T] = {
      Entry[T](
        jEntry.getId,
        jEntry.getUpdated,
        Content[T](jEntry.getContent.getValue, jEntry.getContent.getType),
        jEntry.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList
      )
    }
  }
}
