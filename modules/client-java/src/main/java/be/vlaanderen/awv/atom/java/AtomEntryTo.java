/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.format.Entry;
import be.vlaanderen.awv.atom.format.Link;
import lombok.Data;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;

import java.util.ArrayList;

/**
 * Representation of an entry in an atom feed.
 *
 * @param <T> entry data type
 */
@Data
public class AtomEntryTo<T> {

    private AtomEntryContentTo<T> content;
    private FeedLinkTo[] links;

    /**
     * Converteer naar object dat bruikbaar is voor Atomium.
     *
     * @return atomium feed
     */
    public Entry<T> toAtomium() {
        return new Entry<T>(content.toAtomium(), toFeedLinks(links));
    }

    private List<Link> toFeedLinks(FeedLinkTo[] linkTos) {
        java.util.List<Link> list = new ArrayList<Link>();
        for (FeedLinkTo link : linkTos) {
            list.add(link.toAtomium());
        }
        return JavaConverters.asScalaBufferConverter(list).asScala().toList();
    }

}
