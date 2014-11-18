/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests JSON parsing of Atom feed (with generic entry type).
 */
public class JacksonParseAtomFeedTest {

    private ObjectMapper mapper = new ObjectMapper();
    private static final AtomFeedTo<EventFeedEntryTo> MAPPED_TYPE = new AtomFeedTo<EventFeedEntryTo>();

    @Test
    public void testParseAtomJson() throws Exception {
        String json = FileUtils.readFileToString(
                new File(this.getClass().getClassLoader().getResource(
                        "be/vlaanderen/awv/atom/java/atom-feed-sample.txt").getFile()));

        AtomFeedTo feed = mapper.readValue(json, new TypeReference<AtomFeedTo<EventFeedEntryTo>>() { });

        System.out.println(feed.getEntries()[0].getContent().getRawType());
        System.out.println(feed.getEntries()[0].getContent().getValue().getClass());
        System.out.println(feed.getEntries()[0].getContent().getValue());
        assertThat(feed).isNotNull();
        assertThat(feed.getEntries()[0].getContent().getValue()).isInstanceOf(EventFeedEntryTo.class);
    }

}
