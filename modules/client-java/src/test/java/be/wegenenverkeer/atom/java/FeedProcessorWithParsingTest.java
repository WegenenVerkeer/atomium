/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.wegenenverkeer.atom.java;

import be.wegenenverkeer.atom.EntryRef;
import be.wegenenverkeer.atom.FeedProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.ConfigException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import scala.Option;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class FeedProcessorWithParsingTest {

    private static final String FEED_URL = "http://example.com/feeds/1";

    @Test
    public void test() {
        System.out.println("Start processing");

        // create the feed provider, starting from first item
        ExampleFeedProvider provider = new ExampleFeedProvider();

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
        public Entry<EventFeedEntryTo> accept(Entry<EventFeedEntryTo> entry) {
            System.out.println("Consuming position entry " + entry.getContent());
            try {
                handleEvent(entry.getContent().getValue());
                return entry;
            } catch (Exception e) {
                throw new FeedProcessingException(Option.apply(entry.getId()), e.getMessage());
            }
        }

        public void handleEvent(EventFeedEntryTo event) throws Exception {
            // handle the new event here and persist the current feed position here (possibly in 1 database transaction)
            System.out.println("process feed entry and persist feed position " + event);
        }
    }

    static class ExampleFeedProvider implements FeedProvider<EventFeedEntryTo> {

        private ObjectMapper mapper = new ObjectMapper();

        public ExampleFeedProvider() {
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
                throw new FeedProcessingException(Option.<String>empty(), "not found");
            }

            try {

                String tplFile = "be/wegenenverkeer/atom/java/atom-feed-sample.txt";
                URL resource = getClass().getClassLoader().getResource(tplFile);

                if (resource != null) {

                    String json = FileUtils.readFileToString(new File(resource.getFile()));
                    return mapper.readValue(json, new TypeReference<Feed<EventFeedEntryTo>>() {});

                } else {
                    throw new FeedProcessingException(Option.<String>empty(), "Cannot open template " + tplFile);
                }


            } catch (IOException ioe) {
                throw new FeedProcessingException(Option.<String>empty(), "Cannot open template " + ioe.getMessage());
            }
        }



        @Override
        public EntryRef<EventFeedEntryTo> getInitialEntryRef() {
            // no initial EntryRef for this test
            return null;
        }
    }

}
