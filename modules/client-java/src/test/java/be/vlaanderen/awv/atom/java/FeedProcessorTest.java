package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.*;
import fj.data.Validation;
import org.junit.Test;
import org.junit.Assert;
import scala.Some;

import java.util.ArrayList;
import java.util.List;

public class FeedProcessorTest {

    private static final String FEED_URL = "http://example.com/feeds/";
    private static final String FEED_URL_PAGE1 = FEED_URL + "1";

    @Test
    public void test() {
        System.out.println("Start processing");

        // create the feed position from where you want to start processing
        // index -1 to assure all items are processed, index is position of last read entry in page
        FeedPosition position = new FeedPosition(new Link("self", new Url(FEED_URL_PAGE1)), -1);

        // create the feed provider
        ExampleFeedProvider provider = new ExampleFeedProvider();

        // create your entry consumer
        ExampleEntryConsumer consumer = new ExampleEntryConsumer();

        // create the processor
        FeedProcessor<ExampleFeedEntry> processor = new FeedProcessor<ExampleFeedEntry>(position, provider, consumer);

        // start processing the new feed pages and its entries
        Validation<FeedProcessingError, FeedPosition> result = processor.start();

        if (result.isFail()) {
            Assert.fail(result.fail().message());
        }
        else {
            Assert.assertEquals(FEED_URL_PAGE1, result.success().link().href().path());
        }
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
        public Validation<FeedProcessingError, FeedPosition> consume(FeedPosition position, Entry<ExampleFeedEntry> entry) {
            System.out.println("Consuming position " + position.index() + " entry " + entry.content());
            try {
                handleEvent(entry.content().value().head(), position);
                return Validation.success(position);
            } catch (Exception e) {
                return Validation.fail(new FeedProcessingError(new Some(position), e.getMessage()));
            }
        }

        public void handleEvent(ExampleFeedEntry event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event + " " + position);
        }
    }

    static class ExampleFeedProvider implements FeedProvider<ExampleFeedEntry> {
        @Override
        public Validation<FeedProcessingError, Feed<ExampleFeedEntry>> fetchFeed() {
            throw new UnsupportedOperationException("Should be called with a feed URL.");
        }

        @Override
        public Validation<FeedProcessingError, Feed<ExampleFeedEntry>> fetchFeed(String page) {
            System.out.println("Fetching page " + page);
            int intPage = 0;
            //intPage = Integer.parseInt(page);  // @todo make it fail --- with message "null"
            if (page.endsWith("/1")) intPage=1;
            if (page.endsWith("/2")) intPage=2;
            if (page.endsWith("/3")) intPage=3;

            List<Entry<ExampleFeedEntry>> entries = new ArrayList<Entry<ExampleFeedEntry>>();
            List<ExampleFeedEntry> values1 = new ArrayList<ExampleFeedEntry>();
            values1.add(new ExampleFeedEntry());
            List<ExampleFeedEntry> values2 = new ArrayList<ExampleFeedEntry>();
            values2.add(new ExampleFeedEntry());
            List<ExampleFeedEntry> values3 = new ArrayList<ExampleFeedEntry>();
            values3.add(new ExampleFeedEntry());
            List<Link> links = new ArrayList<Link>();
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(
                    scala.collection.JavaConverters.asScalaBufferConverter(values1).asScala().toList(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(
                    scala.collection.JavaConverters.asScalaBufferConverter(values2).asScala().toList(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(
                    scala.collection.JavaConverters.asScalaBufferConverter(values3).asScala().toList(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));

            List<Link> feedLinks = new ArrayList<Link>();
            feedLinks.add(new Link("first", new Url(FEED_URL_PAGE1)));
            feedLinks.add(new Link("self", new Url(FEED_URL + intPage)));
            if (intPage < 3) {
                feedLinks.add(new Link("next", new Url(FEED_URL + (intPage + 1))));
            }
            if (intPage > 0) {
                feedLinks.add(new Link("previous", new Url(FEED_URL + (intPage - 1))));
            }

            Feed<ExampleFeedEntry> feed = new Feed<ExampleFeedEntry>(
                    "1",
                    new Url(FEED_URL),
                    new Some("Blabla"),
                    "2014-08-08T18:04:14.385+02:00",
                    scala.collection.JavaConverters.asScalaBufferConverter(feedLinks).asScala().toList(),
                    scala.collection.JavaConverters.asScalaBufferConverter(entries).asScala().toList()
            );
            return Validation.success(feed);
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
