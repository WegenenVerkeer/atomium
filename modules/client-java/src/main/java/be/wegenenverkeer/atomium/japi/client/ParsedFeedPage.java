package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.format.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class ParsedFeedPage<E> {
    private final static Logger logger = LoggerFactory.getLogger(ParsedFeedPage.class);

    private final CachedFeedPage<E> page;
    private List<FeedEntry<E>> entries;
    private final FeedPosition feedPosition;

    private ParsedFeedPage(CachedFeedPage<E> page, FeedPosition lastKnownPosition) {
        this.page = page;
        this.feedPosition = lastKnownPosition;
    }

    public CachedFeedPage<E> getPage() {
        return page;
    }

    public List<FeedEntry<E>> getEntries() {
        return entries;
    }

    public static <E> ParsedFeedPage<E> parse(CachedFeedPage<E> page, FeedPosition lastKnownPosition) {
        return new ParsedFeedPage<>(page, lastKnownPosition).parse();
    }

    private ParsedFeedPage<E> parse() {
        List<Entry<E>> entries = new ArrayList<>(page.getEntries());

        if (feedPosition.getEntryId() != null) {
            if (pageHasEntry(page, feedPosition.getEntryId())) {
                logger.debug("Page {} has an entry with ID {}, so we're only emitting items since that ID", feedPosition.getPageUrl(), feedPosition.getEntryId());
                entries = omitOlderOrEqualEntries(entries, feedPosition.getEntryId());
            } else {
                logger.debug("Page {} does not have an entry with ID {}, so we're emitting every item", feedPosition.getPageUrl(), feedPosition.getEntryId());
            }
        }

        this.entries = entries.stream().map(entry -> new FeedEntry<>(entry, page)).collect(Collectors.toList());
        return this;
    }

    private boolean pageHasEntry(CachedFeedPage<E> page, String entryId) {
        return page.getEntries().stream().anyMatch(entry -> entry.getId().equals(entryId));
    }

    private List<Entry<E>> omitOlderOrEqualEntries(List<Entry<E>> entries, String entryId) {
        Collections.reverse(entries);
        List<Entry<E>> cleanedEntries = entries.stream().takeWhile(entry -> !entry.getId().equals(entryId)).collect(Collectors.toList());
        Collections.reverse(cleanedEntries);
        return cleanedEntries;
    }
}
