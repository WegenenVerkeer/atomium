package be.wegenenverkeer.atom

class FeedEntryIterator[E] (feedProvider: FeedPageProvider[E]) extends Iterator[Option[Entry[E]]] {

  type EntryType = E
  type Entries = List[Entry[EntryType]]

  /** Internal pointer to the current entry. */
  private trait EntryCursor {
    def nextCursor : EntryCursor
  }

  private case class InitCursor(feedEntryRef: Option[FeedEntryRef] = None) extends EntryCursor {

    /** Builds the initial EntryCursor
     * This method will search from the start of the feed for the given entry
     */
    def nextCursor : EntryCursor = {
      feedProvider.fetchFeed().map {
        feed => buildCursor(feed, feedEntryRef)
      }.get
    }

  }
  private case class EntryPointer(currentEntry: Entry[EntryType],
                          stillToProcessEntries: Entries,
                          entryRef: FeedEntryRef,
                          feed: Feed[EntryType]) extends EntryCursor {

    /** The next [[EntryCursor]]
      *   - The next [[EntryPointer]] on current page if still entries to be consumed.
      *   - An [[EntryOnPreviousFeedPage]] in case current page is exhausted and a new page is available.
      *   - An [[EndOfEntries]] in case current page is exhausted and no new page is available.
      */
    def nextCursor : EntryCursor = {

      if (stillToProcessEntries.nonEmpty) {
        val nextEntryId = stillToProcessEntries.head.id
        copy(
          currentEntry = stillToProcessEntries.head,
          stillToProcessEntries = stillToProcessEntries.tail, // moving forward, dropping head
          entryRef = entryRef.copy(entryId = nextEntryId) // moving position forward
        )

      } else {
        feed.previousLink match {
          case None => EndOfEntries(Some(entryRef))
          case Some(link) => EntryOnPreviousFeedPage(feed.resolveUrl(link.href))
        }
      }
    }
  }

  private case class EntryOnPreviousFeedPage(previousFeedUrl: Url) extends EntryCursor {

    /** The next [[EntryCursor]].
      *
      * This method will load the previous page an return an [[EntryCursor]].
      */
    def nextCursor : EntryCursor = {
      feedProvider.fetchFeed(previousFeedUrl.path).map {
        feed => buildCursor(feed)
      }.get
    }
  }

  private case class EndOfEntries(lastEntryRef: Option[FeedEntryRef]) extends EntryCursor {
    /** Throws a NonSuchElementException since there is no nextCursor for a [[EndOfEntries]] */
    def nextCursor : EntryCursor = throw new NoSuchElementException("No new entries available!")
  }

  private var cursor: EntryCursor = InitCursor(feedProvider.initialEntryRef)

  private var onGoing = true

  override def hasNext: Boolean = onGoing

  override def next(): Option[Entry[E]] = {

    cursor match {

      case init:InitCursor =>
        updateCursor(init.nextCursor)
        this.next()

      case entryPointer:EntryPointer =>
        updateCursor(entryPointer.nextCursor)
        Some(entryPointer.currentEntry)

      case onPreviousPage: EntryOnPreviousFeedPage =>
        updateCursor(onPreviousPage.nextCursor)
        this.next()

      case end:EndOfEntries => None

    }
  }

  private def updateCursor(cursor:EntryCursor) : Unit = {
    cursor match {
      case end:EndOfEntries => onGoing = false
      case _ => onGoing = true
    }
    this.cursor = cursor
  }

  /**
   * Build an EventCursor according to the following rules:
   *
   * <ul>
   * <li>If there is no FeedEntryRef => create a new cursor on a full Feed with index 0</li>
   * <li>If there is a valid FeedEntryRef => increase index and drop entries preceding new index and create new cursor.</li>
   * <li>If the last element of this page is already consumed
   * <ul>
   * <li>If there is 'previous' Link  create an EntryOnPreviousFeedPage cursor on the next page of the feed</li>
   * <li>If there is no 'previous' Link then create and EndOfEntries cursor.</li>
   * </ul>
   * </li>
   * </ul>
   *
   */
  private def buildCursor(feed: Feed[EntryType], entryRefOpt: Option[FeedEntryRef] = None): EntryCursor = {

    def buildEntryPointer(feed: Feed[EntryType]) : EntryPointer = {
      val entryId = feed.entries.head.id
      EntryPointer(
        feed.entries.head,
        feed.entries.tail,
        FeedEntryRef(feed.resolveUrl(feed.selfLink.href), entryId),
        feed
      )
    }

    def previousFeedOrEnd(feed: Feed[EntryType]): EntryCursor = {
      // we go to previous feed page or we reached the EndOfEntries
      def endOfEntries = {
        entryRefOpt match {
          // rather exceptional situation, can only occurs if a feed is completely empty
          case None => EndOfEntries(None)

          case _ => EndOfEntries(entryRefOpt)

        }
      }

      feed.previousLink match {
        case Some(previousLink) => EntryOnPreviousFeedPage(feed.resolveUrl(previousLink.href))
        case None => endOfEntries
      }

    }


      /** Drop entries from feed page up to the current entry id */
    def dropEntriesUpToEntryRef(entries: List[Entry[E]] ) : Feed[EntryType] = {

      val entryId = entryRefOpt.map(_.entryId)

      val remainingEntries =
        entryId match {
          case Some(id) => entries.filter(_.id != id)
          // no entryId means start of new page
          case None => entries
        }
        feed.copy(entries = remainingEntries)
    }

    val reducedFeedPage = dropEntriesUpToEntryRef(feed.entries)


    if (reducedFeedPage.entries.nonEmpty)
      buildEntryPointer(reducedFeedPage)
    else
      previousFeedOrEnd(reducedFeedPage)

  }



}
