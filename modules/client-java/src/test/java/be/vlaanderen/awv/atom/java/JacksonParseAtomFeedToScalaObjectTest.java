/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Feed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests JSON parsing of Atom feed (with generic entry type).
 */
public class JacksonParseAtomFeedToScalaObjectTest {

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @Ignore
    public void testParseAtomJson() throws Exception {
        String json = FileUtils.readFileToString(
                new File(this.getClass().getClassLoader().getResource(
                        "be/vlaanderen/awv/atom/java/atom-feed-sample.txt").getFile()));

        Feed<EventFeedEntryTo> feed = mapper.readValue(json, new TypeReference<Feed<EventFeedEntryTo>>() {
        });

        System.out.println(feed.entries().head().content().type());
        System.out.println(feed.entries().head().content().value().getClass());
        System.out.println(feed.entries().head().content().value());
        assertThat(feed).isNotNull();
        Assertions.assertThat(feed.entries().head().content().value()).isInstanceOf(EventFeedEntryTo.class);
    }

}
