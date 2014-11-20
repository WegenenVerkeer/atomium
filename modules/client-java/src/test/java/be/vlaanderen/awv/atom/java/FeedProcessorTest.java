package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.*;
import org.joda.time.DateTime;
import org.junit.Test;
import play.libs.Scala;
import scala.Some;
import scala.collection.immutable.HashMap;

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
        FeedProcessor<ExampleFeedEntry> processor = new FeedProcessor<ExampleFeedEntry>(provider, consumer);

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

    static class ExampleEntryConsumer implements EntryConsumer<ExampleFeedEntry> {
        @Override
        public void accept(FeedPosition position, Entry<ExampleFeedEntry> entry) {
            System.out.println("Consuming position " + position.index() + " entry " + entry.content());
            try {
                handleEvent(entry.content().value(), position);
            } catch (Exception e) {
                throw new FeedProcessingException(new Some(position), e.getMessage());
            }
        }

        public void handleEvent(ExampleFeedEntry event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event + " " + position);
        }
    }

    static class ExampleFeedProvider implements FeedProvider<ExampleFeedEntry> {

        /**
         * @return the feed position from where you want to start processing
         * index -1 to assure all items are processed, index is position of last read entry in page
         */
        @Override
        public FeedPosition getInitialPosition() {
            return new FeedPosition(new Url(FEED_URL_PAGE1), -1, new HashMap<String, String>());
        }

        @Override
        public Feed<ExampleFeedEntry> fetchFeed() {
            return fetchFeed(getInitialPosition().url().path());
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
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(new ExampleFeedEntry(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));

            List<Link> feedLinks = new ArrayList<Link>();
            feedLinks.add(new Link("last", new Url(FEED_URL_PAGE1)));
            feedLinks.add(new Link("self", new Url(FEED_URL + intPage)));
            if (intPage < 3) {
                feedLinks.add(new Link("previous", new Url(FEED_URL + (intPage + 1))));
            }
            if (intPage > 0) {
                feedLinks.add(new Link("next", new Url(FEED_URL + (intPage - 1))));
            }

            Feed<ExampleFeedEntry> feed = new Feed<ExampleFeedEntry>(
                    "id",
                    new Url(FEED_URL),
                    new Some("Blabla"),
                    Scala.<Generator>Option(null),
                    new DateTime(),
                    scala.collection.JavaConverters.asScalaBufferConverter(feedLinks).asScala().toList(),
                    scala.collection.JavaConverters.asScalaBufferConverter(entries).asScala().toList(),
                    new HashMap<String, String>()
            );
            return feed;
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
