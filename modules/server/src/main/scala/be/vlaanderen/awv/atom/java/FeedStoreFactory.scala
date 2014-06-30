package be.vlaanderen.awv.atom.java

import be.vlaanderen.awv.atom.Context

// TODO
trait FeedStoreFactory {
  def create[C <: Context](feedName: String, context: C)
}
