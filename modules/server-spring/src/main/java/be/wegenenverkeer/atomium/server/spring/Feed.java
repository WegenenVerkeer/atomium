package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.japi.format.Generator;
import be.wegenenverkeer.atomium.japi.format.Link;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Feed zoals in Atomium, leesbaar en volgens Java 8 standaarden.
 *
 * @param <T>
 */
@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public final class Feed<T> {

    private final String id;
    private final String base;
    private final String title;
    private final Generator generator;
    private final LocalDateTime updated;
    private List<Link> links = new ArrayList<>();
    private List<AtomEntry<T>> entries = new ArrayList<>();

}
