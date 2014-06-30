package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.java.{FeedProvider => JFeedProvider}
import be.vlaanderen.awv.atom.{Feed, FeedProcessingError}
import com.typesafe.scalalogging.slf4j.Logging

import scalaz._


class FeedProviderWrapper[E](underlying: JFeedProvider[E])
  extends be.vlaanderen.awv.atom.FeedProvider[E] with Logging {

  def fetchFeed(): Validation[FeedProcessingError, Feed[E]] = {
    val result = underlying.fetchFeed
    Validations.toScalazValidation(result)
  }

  def fetchFeed(page: String): Validation[FeedProcessingError, Feed[E]] = {
    val result = underlying.fetchFeed(page)
    Validations.toScalazValidation(result)
  }

  override def start(): Unit = underlying.start()

  override def stop(): Unit = underlying.stop()
}