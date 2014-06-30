package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.{Context, Feed, FeedUpdateInfo, FeedInfo}

abstract class FeedStore[E] extends be.vlaanderen.awv.atom.FeedStore[E] {
  def underlying: be.vlaanderen.awv.atom.FeedStore[E]

  override def context: Context = underlying.context

  override def getFeed(page: Long): Option[Feed[E]] = underlying.getFeed(page)

  override def update(feedUpdates: List[FeedUpdateInfo[E]], feedInfo: FeedInfo): Unit = underlying.update(feedUpdates, feedInfo)

  override def getFeedInfo: Option[FeedInfo] = underlying.getFeedInfo
}
