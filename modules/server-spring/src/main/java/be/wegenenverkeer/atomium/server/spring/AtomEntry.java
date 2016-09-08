package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.japi.format.Content;
import be.wegenenverkeer.atomium.japi.format.Link;
import be.wegenenverkeer.atomium.japi.format.pub.AtomPubEntry;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AtomEntry variant which is full java8, using java.time and readable.
 *
 * @param <T>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AtomPubEntry.class, name = "atom-pub"),
        @JsonSubTypes.Type(value = AtomEntry.class, name = "atom")})
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public final class AtomEntry<T> {

    private final String id;
    private final LocalDateTime updated;
    private final Content<T> content;
    private List<Link> links = new ArrayList<>();

}
