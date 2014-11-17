/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.*;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;
import scala.Some;

import java.io.File;
import java.io.IOException;

public class FeedProcessorWithParsingTest {

    private static final String FEED_URL = "http://example.com/feeds/1";

    @Test
    public void test() {
        System.out.println("Start processing");

        // create the feed position from where you want to start processing
        FeedPosition position = new FeedPosition(new Link("self", new Url(FEED_URL)), -1); // postion (-1) is most recently processed

        // create the feed provider
        ExampleFeedProvider provider = new ExampleFeedProvider(position);

        // create your entry consumer
        ExampleEntryConsumer consumer = new ExampleEntryConsumer();

        // create the processor
        FeedProcessor<EventFeedEntryTo>
                processor = new FeedProcessor<EventFeedEntryTo>(provider, consumer);

        // start processing the new feed pages and its entries
        processor.start();

    }

    static class ExampleEntryConsumer implements EntryConsumer<EventFeedEntryTo> {
        @Override
        public void accept(FeedPosition position, Entry<EventFeedEntryTo> entry) {
            System.out.println("Consuming position " + position.index() + " entry " + entry.content());
            try {
                handleEvent(entry.content().value().head(), position);
            } catch (Exception e) {
                throw new FeedProcessingException(new Some(position), e.getMessage());
            }
        }

        public void handleEvent(EventFeedEntryTo event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event + " " + position);
        }
    }

    static class ExampleFeedProvider implements FeedProvider<EventFeedEntryTo> {

        private final FeedPosition initialPostion;
        private ObjectMapper mapper = new ObjectMapper();

        public ExampleFeedProvider(FeedPosition initialPostion) {
            this.initialPostion = initialPostion;
        }

        @Override
        public Feed<EventFeedEntryTo> fetchFeed() {
            System.out.println("fetchFeed");
            return fetchFeed(FEED_URL);
        }

        @Override
        public Feed<EventFeedEntryTo> fetchFeed(String page) {
            System.out.println("Fetching page " + page);

            if (!FEED_URL.equals(page)) {
                throw new FeedProcessingException(Some.<FeedPosition>empty(), "not found");
            }

            try {
                String json = FileUtils.readFileToString(
                        new File(this.getClass().getClassLoader().getResource(
                                "be/vlaanderen/awv/atom/java/atom-feed-sample.txt").getFile()));
                AtomFeedTo<EventFeedEntryTo> feed =
                        mapper.readValue(json, new TypeReference<AtomFeedTo<EventFeedEntryTo>>() {});

                return feed.toAtomium();
            } catch (IOException ioe) {
                throw new FeedProcessingException(Some.<FeedPosition>empty(), "Cannot open template " + ioe.getMessage());
            }
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public FeedPosition getInitialPosition() {
            return initialPostion;
        }
    }

}
