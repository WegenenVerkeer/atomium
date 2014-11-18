package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.format.{FeedContent, Feed}
import be.vlaanderen.awv.atom.{UrlBuilder, Context}

abstract class FeedStore[E <: FeedContent] extends be.vlaanderen.awv.atom.FeedStore[E] {
  def underlying: be.vlaanderen.awv.atom.FeedStore[E]

  override def context: Context = underlying.context

  override def urlProvider: UrlBuilder = underlying.urlProvider

  override def getFeed(start: Int, pageSize: Int): Option[Feed[E]] = underlying.getFeed(start, pageSize)

  override def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = underlying.getHeadOfFeed(pageSize)

  override def push(entries: Iterable[E]) = underlying.push(entries)

}
