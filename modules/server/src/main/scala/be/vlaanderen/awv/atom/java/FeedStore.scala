package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{Context, Feed}

abstract class FeedStore[E] extends be.vlaanderen.awv.atom.FeedStore[E] {
  def underlying: be.vlaanderen.awv.atom.FeedStore[E]

  override def context: Context = underlying.context

  override def getFeed(start: Int, pageSize: Int): Option[Feed[E]] = underlying.getFeed(start, pageSize)

  override def getHeadOfFeed(pageSize: Int): Option[Feed[E]] = underlying.getHeadOfFeed(pageSize)

  override def push(entries: Iterable[E]) = underlying.push(entries)

}
