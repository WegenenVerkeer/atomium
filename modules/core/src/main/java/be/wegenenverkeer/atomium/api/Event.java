package be.wegenenverkeer.atomium.api;

import be.wegenenverkeer.atomium.format.AtomEntry;
import be.wegenenverkeer.atomium.format.Content;
import be.wegenenverkeer.atomium.format.Entry;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by Karel Maesen, Geovise BVBA on 14/12/16.
 */
public class Event<T> {

    private final T value;
    private final String id;
    private final OffsetDateTime updated;

    public Event(String id, T value, OffsetDateTime updated) {
        if (id == null || value == null || updated == null) {
            throw new IllegalArgumentException("Require non-null arguments");
        }
        this.id = id;
        this.value = value;
        this.updated = updated;
    }

    public T getValue() {
        return value;
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getUpdated() {
        return updated;
    }

    public static <T> Event<T> make(String id, T value, OffsetDateTime updated){
        return new Event<>(id, value, updated);
    }

    public Entry<T> toAtomEntry() {
        //TODO -- what with AtomPubEntry???
        return new AtomEntry<T>(id, updated, new Content<>(value, ""));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Event<?> event = (Event<?>) o;

        if (!value.equals(event.value)) return false;
        if (!id.equals(event.id)) return false;
        //this is used because comparing events between databases may result in spurious errors if
        //compared exactly
        return Math.abs(Duration.between(updated, event.updated).toMillis()) < 100;
    }

    @Override
    public int hashCode() {
        int result = value.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + updated.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Event{" +
                "value=" + value +
                ", id='" + id + '\'' +
                ", updated=" + updated +
                '}';
    }
}
