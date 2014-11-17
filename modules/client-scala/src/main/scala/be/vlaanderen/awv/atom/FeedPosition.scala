package be.vlaanderen.awv.atom

/**
 * The feed position contains the data that indicates which entry was last consumed.
 *
 * @param link the link to last retrieved page
 * @param index the index of the last item consumed on the last retrieved page
 */
case class FeedPosition(link: Link, index: Int)