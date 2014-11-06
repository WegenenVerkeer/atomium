package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.*;
import org.junit.Test;
import scala.Some;

import java.util.ArrayList;
import java.util.List;

public class FeedProcessorTest {

    private static final String FEED_URL_PAGE1 = "http://example.com/feeds/1";

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
        processor.start();
    }

    class ExampleFeedEntry {

    }

    class ExampleEntryConsumer implements EntryConsumer<ExampleFeedEntry> {
        @Override
        public void consume(FeedPosition position, Entry<ExampleFeedEntry> entry) {
            System.out.println("Consuming position " + position.index());
            try {
                handleEvent(entry.content().value().head(), position);
            } catch (Exception e) {
                throw new FeedProcessingException(new Some(position), e.getMessage());
            }
        }

        public void handleEvent(ExampleFeedEntry event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
        }
    }

    class ExampleFeedProvider implements FeedProvider<ExampleFeedEntry> {
        @Override
        public Feed<ExampleFeedEntry> fetchFeed() {
            return fetchFeed("http://example.com/feeds/1");
        }

        @Override
        public Feed<ExampleFeedEntry> fetchFeed(String url) {
            System.out.println("Fetching page " + url);

            List<Entry<ExampleFeedEntry>> entries = new ArrayList<Entry<ExampleFeedEntry>>();
            List<ExampleFeedEntry> values = new ArrayList<ExampleFeedEntry>();
            values.add(new ExampleFeedEntry());
            List<Link> links = new ArrayList<Link>();
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(
                    scala.collection.JavaConverters.asScalaBufferConverter(values).asScala().toList(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));

            List<Link> feedLinks = new ArrayList<Link>();
            feedLinks.add(new Link("self", new Url(url)));

            Feed<ExampleFeedEntry> feed = new Feed<ExampleFeedEntry>(
                    "1",
                    new Url("http://example.com/feeds"),
                    new Some("Blabla"),
                    "2014-08-08T18:04:14.385+02:00",
                    scala.collection.JavaConverters.asScalaBufferConverter(feedLinks).asScala().toList(),
                    scala.collection.JavaConverters.asScalaBufferConverter(entries).asScala().toList()
            );
            return feed;
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }
}
