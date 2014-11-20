/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Entry;
import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.Link;
import be.vlaanderen.awv.atom.Url;
import lombok.Data;
import scala.Some;
import scala.collection.JavaConverters;
import scala.collection.immutable.HashMap;
import scala.collection.immutable.List;

import java.util.ArrayList;

/**
 * Representation of an atom feed.
 *
 * @param <T> entry data type
 */
@Data
public class AtomFeedTo<T> {

    private String id;
    private String base; // base URL
    private String title;
    private String updated;
    private FeedLinkTo[] links;
    private AtomEntryTo<T>[] entries;

    /**
     * Converts to an object usable by Atomium.
     *
     * @return atomium feed
     */
    public Feed<T> toAtomium() {

        return new Feed<T>(
                id,
                new Url(base),
                new Some(title),
                updated,
                toFeedLinks(links),
                toFeedEntries(entries),
                new HashMap<String, String>()
        );
    }

    private List<Link> toFeedLinks(FeedLinkTo[] linkTos) {
        java.util.List<Link> list = new ArrayList<Link>();
        for (FeedLinkTo link : linkTos) {
            list.add(link.toAtomium());
        }
        return JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

    private List<Entry<T>> toFeedEntries(AtomEntryTo[] entryTos) {
        java.util.List<Entry<T>> list = new ArrayList<Entry<T>>();
        for (AtomEntryTo entry : entryTos) {
            list.add(entry.toAtomium());
        }
        return JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

}
