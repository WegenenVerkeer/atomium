package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.format.TimestampFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TimestampFormatTest {

    private final String date;

    public TimestampFormatTest(String date) {
        this.date = date;
    }

    @Parameterized.Parameters
    public static Collection<String> dateSource() {
        return Arrays.asList(
                "2024-06-05T12:00:36.191979123+02:00",
                "2024-06-05T12:00:36.191979+02:00",
                "2024-06-05T12:00:36.191+02:00",
                "2024-06-05T12:00:36+02:00",
                "2024-06-05T12:00:36.191979123+0200",
                "2024-06-05T12:00:36.191979+0200",
                "2024-06-05T12:00:36.191+0200",
                "2024-06-05T12:00:36+0200",
                "2024-06-05T12:00:36.191979123+02",
                "2024-06-05T12:00:36.191979+02",
                "2024-06-05T12:00:36.191+02",
                "2024-06-05T12:00:36+02"
        );
    }

    @Test
    public void parse() {
        try {
            TimestampFormat.parse(date);
        } catch (Exception e) {
            assertTrue("Expected no exception, but got: " + e.getMessage(), false);
        }
    }
}