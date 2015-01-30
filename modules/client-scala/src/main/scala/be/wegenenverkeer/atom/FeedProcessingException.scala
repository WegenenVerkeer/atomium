package be.wegenenverkeer.atom

case class FeedProcessingException(entryIdOpt:Option[String], message:String) extends RuntimeException {

  /**
   * Returns the entry id or null if none is available.
   * (For easy access from Java API)
   */
  def entryId = entryIdOpt.orNull

  override def getMessage: String = message
}
