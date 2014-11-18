package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.FeedPosition
import be.vlaanderen.awv.atom.format._
import be.vlaanderen.awv.atom.java.{FeedProvider => JFeedProvider}
import com.typesafe.scalalogging.slf4j.Logging

import scala.util.{Failure, Success, Try}

class FeedProviderWrapper[E <: FeedContent](underlying: JFeedProvider[E])
  extends be.vlaanderen.awv.atom.FeedProvider[E] with Logging {

  override def fetchFeed(): Try[Feed[E]] = {
    try {
      Success(underlying.fetchFeed)
    } catch {
      case ex:Exception => Failure(ex)
    }
  }

  def fetchFeed(page: String): Try[Feed[E]] = {
    try {
      Success(underlying.fetchFeed(page))
    } catch {
      case ex:Exception =>
        Failure(ex)
    }
  }

  override def start(): Unit = underlying.start()

  override def stop(): Unit = underlying.stop()

  override def initialPosition: Option[FeedPosition] = Option.apply(underlying.getInitialPosition)
}