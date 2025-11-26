package be.wegenenverkeer.atomium.client;

import be.wegenenverkeer.atomium.client.rxhttpclient.RxHttpAtomiumClient;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import be.wegenenverkeer.rxhttpclient.rxjava.RxJavaHttpClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Test;

public class BuilderTest {

    @Test
    public void testBuilderCanBeConfiguredWithCustomFeedpageCodec() {
        var client = new RxHttpAtomiumClient(new RxJavaHttpClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .build());

        var codec = new JacksonFeedPageCodec<>(Event.class);
        codec.registerModules();
        codec.configureObjectMapper(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        codec.configureObjectMapper(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, false);

        var pageFetcher = client.getPageFetcherBuilder("/feeds/events/", Event.class)
                .setCodec( codec )
                .build();

        client.feed(pageFetcher);

    }
}
