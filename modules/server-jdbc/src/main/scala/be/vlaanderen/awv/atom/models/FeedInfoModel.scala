package be.vlaanderen.awv.atom.models

case class FeedInfoModel(
  id: Option[Long] = None,
  feedName: String,
  lastPageId: Long,
  count: Int)
