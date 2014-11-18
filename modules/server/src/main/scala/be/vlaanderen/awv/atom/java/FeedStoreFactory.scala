package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context
import be.vlaanderen.awv.atom.format.FeedContent

trait FeedStoreFactory[E <: FeedContent, C <: Context] {
  def create(feedName: String, context: C): FeedStore[E]
}
