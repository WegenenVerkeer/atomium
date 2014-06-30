package be.vlaanderen.awv.atom

case class FeedProcessingError(feedPositionOpt:Option[FeedPosition], message:String) {

  /**
   * Returns the {{{FeedPosition}}} or null if none is available.
   * (For easy access from Java API)
   */
  def feedPosition = feedPositionOpt.orNull
}