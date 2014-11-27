/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.JFeed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests JSON parsing of Atom feed (with generic entry type).
 */
public class JacksonParseAtomFeedTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testParseAtomJson() throws Exception {
        String json = FileUtils.readFileToString(
                new File(this.getClass().getClassLoader().getResource(
                        "be/vlaanderen/awv/atom/java/atom-feed-sample.txt").getFile()));

        JFeed<EventFeedEntryTo> feed = mapper.readValue(json, new TypeReference<JFeed<EventFeedEntryTo>>() { });

        assertThat(feed).isNotNull();
        System.out.println(feed.getEntries().get(0).getContent().getType());
        System.out.println(feed.getEntries().get(0).getContent().getValue().getClass());
        System.out.println(feed.getEntries().get(0).getContent().getValue());
        assertThat(feed.getEntries().get(0).getContent().getValue()).isInstanceOf(EventFeedEntryTo.class);
    }

}
