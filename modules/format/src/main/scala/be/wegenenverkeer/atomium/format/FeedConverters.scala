package be.wegenenverkeer.atomium.format

import be.wegenenverkeer.atomium.format.pub._
import be.wegenenverkeer.atomium.japi.format

import scala.collection.JavaConverters._


object FeedConverters {

  implicit class Feed2JFeed[T](feed: Feed[T]) {

    def asJava: Feed[T] = {
      new Feed[T](
        feed.id,
        feed.base.path,
        feed.title.orNull,
        feed.generator.map(_.asJava).orNull,
        feed.updated,
        feed.links.map(l => new Link(l.rel, l.href.path)).asJava,
        feed.entries.map(_.asJava).asJava
      )
    }
  }

  implicit class Generator2JGenerator(generator: Generator) {

    def asJava: Generator = {
      new Generator(
        generator.text,
        generator.uri.map(u => u.path).orNull,
        generator.version.orNull
      )
    }
  }

  implicit class Entry2JEntry[T](entry: Entry[T]) {

    def asJava: Entry[T] = {
      entry match {
        case atomPubEntry: AtomPubEntry[T] => new pub.AtomPubEntry[T](
          atomPubEntry.id,
          atomPubEntry.updated,
          new Content[T](atomPubEntry.content.value, atomPubEntry.content.`type`),
          atomPubEntry.links.map(l => new Link(l.rel, l.href.path)).asJava,
          atomPubEntry.edited,
          atomPubEntry.control.asJava
        )
        case atomEntry: AtomEntry[T]       => new AtomEntry[T](
          atomEntry.id,
          atomEntry.updated,
          new Content[T](atomEntry.content.value, atomEntry.content.`type`),
          atomEntry.links.map(l => new Link(l.rel, l.href.path)).asJava
        )
      }
    }
  }


  implicit class PubControl2JPubControl(control: Control) {

    def asJava: Control = {
      val draft = if (control.draft == DraftYes) Draft.YES else pub.Draft.NO
      new Control(draft)
    }
  }

  implicit class JFeed2Feed[T](jfeed: Feed[T]) {

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

  implicit class JGenerator2OptionGenerator(jGenerator: Generator) {

    def asScalaOpt: Option[Generator] = {
      jGenerator match {
        case null => None
        case _    => Some(Generator(jGenerator.getText, Option(jGenerator.getUri).map(p => Url(p)), Option(jGenerator.getVersion)))
      }
    }
  }

  implicit class JPubControl2PubControl(jControl: Control) {

    def asScala: Control = {
      val draft = if (jControl.getDraft == pub.Draft.YES) DraftYes else DraftNo
      Control(draft)
    }
  }


  implicit class JEntry2Entry[T](jEntry: Entry[T]) {

    def asScala: Entry[T] = {

      jEntry match {

        case atomPubEntry: AtomPubEntry[T] =>
          AtomPubEntry(
            atomPubEntry.getId,
            atomPubEntry.getUpdated,
            Content(atomPubEntry.getContent.getValue, atomPubEntry.getContent.getType),
            atomPubEntry.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList,
            atomPubEntry.getEdited,
            atomPubEntry.getControl.asScala
          )

        case atomEntry: AtomEntry[T] =>
          AtomEntry(
            atomEntry.getId,
            atomEntry.getUpdated,
            Content(atomEntry.getContent.getValue, atomEntry.getContent.getType),
            atomEntry.getLinks.asScala.map(l => Link(l.getRel, Url(l.getHref))).toList
          )
      }

    }
  }

}
