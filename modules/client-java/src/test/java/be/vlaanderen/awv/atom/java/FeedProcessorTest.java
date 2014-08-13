package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.*;
import fj.data.Validation;
import org.junit.Test;
import org.junit.Assert;
import scala.Some;

import java.util.ArrayList;
import java.util.List;

public class FeedProcessorTest {

    @Test
    public void test() {
        System.out.println("Start processing");

        // create the feed position from where you want to start processing
        FeedPosition position = new FeedPosition(new Link("self", new Url("http://example.com/feeds/1")), 1);

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
            Assert.assertEquals("http://example.com/feeds/1", result.success().link().href().path());
        }
    }

    class ExampleFeedEntry {

    }

    class ExampleEntryConsumer implements EntryConsumer<ExampleFeedEntry> {
        @Override
        public Validation<FeedProcessingError, FeedPosition> consume(FeedPosition position, Entry<ExampleFeedEntry> entry) {
            System.out.println("Consuming position " + position.index());
            try {
                handleEvent(entry.content().value().head(), position);
                return Validation.success(position);
            } catch (Exception e) {
                return Validation.fail(new FeedProcessingError(new Some(position), e.getMessage()));
            }
        }

        public void handleEvent(ExampleFeedEntry event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
        }
    }

    class ExampleFeedProvider implements FeedProvider<ExampleFeedEntry> {
        @Override
        public Validation<FeedProcessingError, Feed<ExampleFeedEntry>> fetchFeed() {
            return fetchFeed("1");
        }

        @Override
        public Validation<FeedProcessingError, Feed<ExampleFeedEntry>> fetchFeed(String page) {
            System.out.println("Fetching page " + page);

            List<Entry<ExampleFeedEntry>> entries = new ArrayList<Entry<ExampleFeedEntry>>();
            List<ExampleFeedEntry> values = new ArrayList<ExampleFeedEntry>();
            values.add(new ExampleFeedEntry());
            List<Link> links = new ArrayList<Link>();
            entries.add(new Entry<ExampleFeedEntry>(new Content<ExampleFeedEntry>(
                    scala.collection.JavaConverters.asScalaBufferConverter(values).asScala().toList(), ""),
                    scala.collection.JavaConverters.asScalaBufferConverter(links).asScala().toList()));

            List<Link> feedLinks = new ArrayList<Link>();
            feedLinks.add(new Link("self", new Url("http://example.com/feeds/1")));

            Feed<ExampleFeedEntry> feed = new Feed<ExampleFeedEntry>(
                    "1",
                    new Url("http://example.com/feeds"),
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
