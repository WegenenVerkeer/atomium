package be.vlaanderen.awv.atom

case class FeedProcessingError(feedPos:Option[FeedPosition], message:String)