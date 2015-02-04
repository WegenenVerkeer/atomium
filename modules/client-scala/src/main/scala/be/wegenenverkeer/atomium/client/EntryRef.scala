package be.wegenenverkeer.atomium.client

import _root_.java.net.URI

import be.wegenenverkeer.atomium.format.{Entry, Feed, Url}

/** The feed entry id reference contains the data that indicates which entry was last consumed.
  *
  * @param entryId the entry id of the last consumed item
  * @param url the url of the last retrieved page
  */
case class EntryRef[E](entryId: String, url: Url, entry: Option[Entry[E]]) {

  require(new URI(url.path).isAbsolute)
}


object EntryRef {

  def apply[E](entryId: String, url: Url): EntryRef[E] = {
    new EntryRef[E](entryId, url, None)
  }

  /** Builds an [[EntryRef]] from a given [[Feed]] and [[Entry]]
    *
    * @param feed -  a non-empty `Feed` containing the `Entry`
    * @param entry - the `Entry`
    * @tparam E - the type of the `Entry` content
    */
  @throws[IllegalArgumentException]("if given `Entry` is not present in the `Feed`")
  def apply[E](feed: Feed[E], entry: Entry[E]): EntryRef[E] = {

    require(feed.entries.exists(_.id == entry.id), "Given Entry must be present in Feed")

    EntryRef(entry.id, feed.resolveUrl(feed.selfLink.href), Some(entry))
  }


  /** Builds an [[EntryRef]] from a given [[Feed]].
    *
    * The `EntryLoc` will reference the first `Entry` in the `Feed`
    *
    * @param feed - a non-empty `Feed`
    * @tparam E - the type of the `Entry` content
    */
  @throws[IllegalArgumentException]("if `Feed` is empty")
  def apply[E](feed: Feed[E]): EntryRef[E] = {

    require(feed.entries.nonEmpty, "Feed can't be empty")

    val entry = feed.entries.head
    EntryRef(entry.id, feed.resolveUrl(feed.selfLink.href), Some(entry))
  }


}
