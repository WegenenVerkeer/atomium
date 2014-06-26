package be.vlaanderen.awv.atom.javaapi

import be.vlaanderen.awv.atom.Feed
import be.vlaanderen.awv.atom.javaapi.{ FeedProvider => JFeedProvider}

import scalaz._
import Scalaz._


class FeedProviderWrapper[E](javaFeedProvider: JFeedProvider[E])
  extends be.vlaanderen.awv.atom.FeedProvider[E] {

  def fetchFeed(): ValidationNel[String, Feed[E]] =
    fetch(javaFeedProvider.fetchFeed)

  def fetchFeed(page: String): ValidationNel[String, Feed[E]] =
    fetch(javaFeedProvider.fetchFeed(page))

  private def fetch(block: => Feed[E]) : ValidationNel[String, Feed[E]] = {
    try {
      block.successNel[String]
    }
    catch {
      case ex: Exception =>
        ex.getMessage.failNel[Feed[E]]
    }
  }
}