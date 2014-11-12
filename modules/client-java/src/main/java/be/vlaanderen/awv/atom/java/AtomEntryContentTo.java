/*
 * Dit bestand is een onderdeel van AWV DistrictCenter.
 * Copyright (c) AWV Agentschap Wegen en Verkeer, Vlaamse Gemeenschap
 */

package be.vlaanderen.awv.atom.java;

import be.vlaanderen.awv.atom.format.Content;
import lombok.Data;
import scala.collection.JavaConverters;

import java.util.Arrays;

/**
 * Representation of an entry in an atom feed.
 *
 * @param <T> entry data type
 */
@Data
public class AtomEntryContentTo<T> {

    private T[] value;
    private String rawType;

    /**
     * Converts to an object usable by Atomium.
     *
     * @return atomium feed
     */
    public Content<T> toAtomium() {
        return new Content<T>(JavaConverters.asScalaBufferConverter(Arrays.asList(value)).asScala().toList(), rawType);

    }

}
