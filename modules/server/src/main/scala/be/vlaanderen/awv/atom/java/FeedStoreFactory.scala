package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context

trait FeedStoreFactory[E, C <: Context] {
  def create(feedName: String, context: C): FeedStore[E]
}
