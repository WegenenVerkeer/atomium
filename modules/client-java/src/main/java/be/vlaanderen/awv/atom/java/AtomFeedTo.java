/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.format.Entry;
import be.vlaanderen.awv.atom.format.Feed;
import be.vlaanderen.awv.atom.format.Link;
import be.vlaanderen.awv.atom.format.Url;
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

    private String base; // base URL
    private String title;
    private String updated;
    private FeedLinkTo[] links;
    private AtomEntryTo<T>[] entries;

    /**
     * Converteer naar object dat bruikbaar is voor Atomium.
     *
     * @return atomium feed
     */
    public Feed<T> toAtomium() {

        return new Feed<T>(
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
