package be.wegenenverkeer.atomium.server.spring;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;

@Data
@RequiredArgsConstructor
public class TestFeedEntry {
    private final long id;
    private final OffsetDateTime timestamp;
}