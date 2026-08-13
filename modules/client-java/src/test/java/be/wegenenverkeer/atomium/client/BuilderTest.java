package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Test;

public class BuilderTest {

    /**
     * Configuring the ObjectMapper is done by subclassing: the mapper is visible to subclasses.
     */
    static class EventFeedPageCodec extends JacksonFeedPageCodec<Event> {
        EventFeedPageCodec() {
            super(Event.class);
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, false);
        }
    }

    @Test
    public void testBuilderCanBeConfiguredWithCustomFeedpageCodec() {
        var client = new RxHttpAtomiumClient(new RxJavaHttpClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .build());

        var codec = new EventFeedPageCodec();
        codec.registerModules();

        var pageFetcher = client.getPageFetcherBuilder("/feeds/events/", Event.class)
                .setCodec( codec )
                .build();

        client.feed(pageFetcher);

    }
}
