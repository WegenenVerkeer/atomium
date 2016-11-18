package be.wegenenverkeer.atomium.format;

import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * Created by Karel Maesen, Geovise BVBA on 22/08/16.
 */
public class AdaptersTest {


    @Test
    public void testSerializationOfDateTime(){
        ZonedDateTime dt = ZonedDateTime.of(2016, 8, 22, 16, 20, 6, 0, TimeZone.getTimeZone("Europe/Brussels").toZoneId());
        String dtText = TimestampFormat.format(dt.toOffsetDateTime());
        assertEquals("2016-08-22T16:20:06+02:00", dtText);
    }

    @Test
    public void testSerializationOfUTCDateTime(){
        OffsetDateTime dt = OffsetDateTime.of(2016, 8, 22, 16, 20, 6, 0, ZoneOffset.ofHours(0));
        String dtText = TimestampFormat.format(dt);
        assertEquals("2016-08-22T16:20:06Z", dtText);
    }

    @Test
    public void testDeSerializationOfDateTime(){
        ZonedDateTime dt = ZonedDateTime.of(2016, 8, 22, 16, 20, 6, 0, TimeZone.getTimeZone("Europe/Brussels").toZoneId());
        OffsetDateTime parsed = TimestampFormat.parse("2016-08-22T16:20:06+02:00");
        assertEquals(dt.toOffsetDateTime(), parsed);
    }

    @Test
    public void testDeSerializationOfUTCDateTime(){
        OffsetDateTime dt = OffsetDateTime.of(2016, 8, 22, 16, 20, 6, 0, ZoneOffset.ofHours(0));
        OffsetDateTime parsed = TimestampFormat.parse("2016-08-22T16:20:06Z");
        assertEquals(dt, parsed);
    }

    @Test
    public void testDeserializatoinOfVariantDateTime(){
        OffsetDateTime dt = OffsetDateTime.of(2015, 8, 26, 19, 41, 44, 0, ZoneOffset.ofHours(2));
        OffsetDateTime parsed = TimestampFormat.parse("2015-08-26T19:41:44+0200");
        assertEquals(dt, parsed);
    }



}
