package be.wegenenverkeer.atom.java;

import be.wegenenverkeer.atom.EntryRef;
import be.wegenenverkeer.atom.FeedProcessingException;
import be.wegenenverkeer.atom.Url;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import scala.Option;

import java.util.ArrayList;
import java.util.List;

public class FeedProcessorTest {

    private static final String FEED_URL = "http://example.com/feeds/";
    private static final String FEED_URL_PAGE1 = FEED_URL + "1";

    @Test
    public void test() {
        System.out.println("Start processing");

        // create the feed provider
        ExampleFeedProvider provider = new ExampleFeedProvider();

        // create your entry consumer
        ExampleEntryConsumer consumer = new ExampleEntryConsumer();

        // create the processor
        be.wegenenverkeer.atom.java.FeedProcessor<ExampleFeedEntry> processor = new be.wegenenverkeer.atom.java.FeedProcessor<ExampleFeedEntry>(provider, consumer);

        // start processing the new feed pages and its entries
        processor.start();
    }

    static class ExampleFeedEntry {
        private static int COUNTER;
        private int counter;

        ExampleFeedEntry() {
            this.counter = COUNTER++;
        }

        @Override
        public String toString() {
            return "ExampleFeedEntry{" +
                    "counter=" + counter +
                    '}';
        }
    }

    static class ExampleEntryConsumer implements be.wegenenverkeer.atom.java.EntryConsumer<ExampleFeedEntry> {
        @Override
        public Entry<ExampleFeedEntry> accept(Entry<ExampleFeedEntry> entry) {
            System.out.println("Consuming entry " + entry.getContent());
            try {
                handleEvent(entry.getContent().getValue());
                return entry;
            } catch (Exception e) {
                throw new FeedProcessingException(Option.apply(entry.getId()), e.getMessage());
            }
        }

        public void handleEvent(ExampleFeedEntry event) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event);
        }
    }

    static class ExampleFeedProvider implements be.wegenenverkeer.atom.java.FeedProvider<ExampleFeedEntry> {

        /**
         * @return the EntryRef from where you want to start processing
         * A null EntryRef assure that all items will be processed.
         */
        @Override
        public EntryRef<ExampleFeedEntry> getInitialEntryRef() {
            return null;
        }

        @Override
        public Feed<ExampleFeedEntry> fetchFeed() {
            return fetchFeed(FEED_URL_PAGE1);
        }

        @Override
        public Feed<ExampleFeedEntry> fetchFeed(String page) {
            System.out.println("Fetching page " + page);
            int intPage = 0;
            //intPage = Integer.parseInt(page);  // @todo make it fail --- with message "null"
            if (page.endsWith("/1")) intPage=1;
            if (page.endsWith("/2")) intPage=2;
            if (page.endsWith("/3")) intPage=3;

            List<Entry<ExampleFeedEntry>> entries = new ArrayList<Entry<ExampleFeedEntry>>();
            List<Link> links = new ArrayList<Link>();
            entries.add(new Entry<ExampleFeedEntry>("id1", new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""), links));
            entries.add(new Entry<ExampleFeedEntry>("id2", new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""), links));
            entries.add(new Entry<ExampleFeedEntry>("id3", new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""), links));

            List<Link> feedLinks = new ArrayList<Link>();
            feedLinks.add(new Link("last", FEED_URL_PAGE1));
            feedLinks.add(new Link("self", FEED_URL + intPage));
            if (intPage < 3) {
                feedLinks.add(new Link("previous", FEED_URL + (intPage + 1)));
            }
            if (intPage > 0) {
                feedLinks.add(new Link("next", FEED_URL + (intPage - 1)));
            }

            Feed<ExampleFeedEntry> feed = new Feed<ExampleFeedEntry>(
                    "id",
                    FEED_URL,
                    "Blabla",
                    null,
                    new LocalDateTime(),
                feedLinks,
                    entries
            );
            return feed;
        }

    }
}
