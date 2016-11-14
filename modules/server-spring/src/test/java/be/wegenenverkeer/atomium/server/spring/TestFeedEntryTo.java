package be.wegenenverkeer.atomium.server.spring;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestFeedEntryTo {
    private final long id;
}
