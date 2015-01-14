package be.wegenenverkeer.atom

import _root_.java.net.URI

/**
 * The feed entry reference contains the data that indicates which entry was last consumed.
 *
 * @param feedUrl the url of the last retrieved page
 * @param entry the entry of the last consumed item
 */
case class EntryLoc[E](entry: Entry[E], feedUrl: Url) {
  require(new URI(feedUrl.path).isAbsolute)
}

object EntryLoc {


  /** Builds an [[EntryLoc]] from a given [[Feed]] and [[Entry]]
    *
    * @param feed -  a non-empty `Feed` containing the `Entry`
    * @param entry - the `Entry`
    * @tparam E - the type of the `Entry` content
    */
  @throws[IllegalArgumentException]("if given `Entry` is not present in the `Feed`")
  def apply[E](feed:Feed[E], entry:Entry[E]) : EntryLoc[E] = {

    require(feed.entries.exists(_.id == entry.id), "Given Entry must be present in Feed")

    EntryLoc(entry, feed.resolveUrl(feed.selfLink.href))
  }



  /** Builds an [[EntryLoc]] from a given [[Feed]].
    *
    * The `EntryLoc` will reference the first `Entry` in the `Feed`
    *
    * @param feed - a non-empty `Feed`
    * @tparam E - the type of the `Entry` content
    */
  @throws[IllegalArgumentException]("if `Feed` is empty")
  def apply[E](feed:Feed[E]) : EntryLoc[E] = {

    require(feed.entries.nonEmpty, "Feed can't be empty")

    EntryLoc(feed.entries.head, feed.resolveUrl(feed.selfLink.href))
  }
}