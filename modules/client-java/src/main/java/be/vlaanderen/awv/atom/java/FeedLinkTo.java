/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.Link;
import be.vlaanderen.awv.atom.Url;
import lombok.Data;

/**
 * Representation of a link in an atom feed.
 */
@Data
public class FeedLinkTo {

    private String rel; // relation, e.g."self", "next", previous"
    private String href; // link URL

    /**
     * Converts to an object usable by Atomium.
     *
     * @return atomium feed
     */
    public Link toAtomium() {
        return new Link(rel, new Url(href));
    }

}
