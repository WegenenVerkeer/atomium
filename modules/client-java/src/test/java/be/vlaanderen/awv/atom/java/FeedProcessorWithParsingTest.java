/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Entry;
import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.FeedPosition;
import be.vlaanderen.awv.atom.FeedProcessingError;
import be.vlaanderen.awv.atom.Link;
import be.vlaanderen.awv.atom.Url;
import fj.data.Validation;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Assert;
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
        FeedPosition position = new FeedPosition(new Link("self", new Url(FEED_URL)), -1); // postion (-1) is meest recent verwerkte

        // create the feed provider
        ExampleFeedProvider provider = new ExampleFeedProvider();

        // create your entry consumer
        ExampleEntryConsumer consumer = new ExampleEntryConsumer();

        // create the processor
        FeedProcessor<EventFeedEntryTo>
                processor = new FeedProcessor<EventFeedEntryTo>(position, provider, consumer);

        // start processing the new feed pages and its entries
        Validation<FeedProcessingError, FeedPosition> result = processor.start();

        if (result.isFail()) {
            System.out.println(result.fail());
            System.out.println(result.fail().message());
            System.out.println(result.fail().feedPosition());
            System.out.println(result.fail().productIterator());
            System.out.println(result.fail().productPrefix());
            System.out.println(result.fail().productPrefix());
            Assert.fail(result.fail().message());
        }
        else {
            Assertions.assertThat(result.success().link().href().path()).
                    isEqualTo("http://sample.com/feeds/registraties/feed/1");
        }
    }

    static class ExampleEntryConsumer implements EntryConsumer<EventFeedEntryTo> {
        @Override
        public Validation<FeedProcessingError, FeedPosition> consume(FeedPosition position, Entry<EventFeedEntryTo> entry) {
            System.out.println("Consuming position " + position.index() + " entry " + entry.content());
            try {
                handleEvent(entry.content().value().head(), position);
                return Validation.success(position);
            } catch (Exception e) {
                return Validation.fail(new FeedProcessingError(new Some(position), e.getMessage()));
            }
        }

        public void handleEvent(EventFeedEntryTo event, FeedPosition position) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event + " " + position);
        }
    }

    static class ExampleFeedProvider implements FeedProvider<EventFeedEntryTo> {

        private ObjectMapper mapper = new ObjectMapper();

        @Override
        public Validation<FeedProcessingError, Feed<EventFeedEntryTo>> fetchFeed() {
            System.out.println("fetchFeed");
            return fetchFeed(FEED_URL);
        }

        @Override
        public Validation<FeedProcessingError, Feed<EventFeedEntryTo>> fetchFeed(String page) {
            System.out.println("Fetching page " + page);

            if (!FEED_URL.equals(page)) {
                return Validation.fail(new FeedProcessingError(Some.<FeedPosition>empty(), "not found"));
            }

            try {
                String json = FileUtils.readFileToString(
                        new File(this.getClass().getClassLoader().getResource(
                                "be/vlaanderen/awv/atom/java/atom-feed-sample.txt").getFile()));
                AtomFeedTo<EventFeedEntryTo> feed =
                        mapper.readValue(json, new TypeReference<AtomFeedTo<EventFeedEntryTo>>() {});

                return Validation.success(feed.toAtomium());
            } catch (IOException ioe) {
                return Validation.fail(new FeedProcessingError(Some.<FeedPosition>empty(),
                        "Cannot open template " + ioe.getMessage()));
            }
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}
    }

}
