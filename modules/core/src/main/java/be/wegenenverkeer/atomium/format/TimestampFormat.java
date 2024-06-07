package be.wegenenverkeer.atomium.format;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;

/**
 * Created by Karel Maesen, Geovise BVBA on 18/11/16.
 */
public class TimestampFormat {

    public static java.time.format.DateTimeFormatter WRITE_FORMAT =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .appendPattern("XXXXX")
                    .toFormatter();

    public static java.time.format.DateTimeFormatter PARSE_FORMAT =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .optionalStart()
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .optionalEnd()
                    .optionalStart()
                    .appendOffsetId()
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern("X")
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern("XX")
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern("XXX")
                    .optionalEnd()
                    .toFormatter();


    public static OffsetDateTime parse(String v)  {
        return OffsetDateTime.parse(v, PARSE_FORMAT);
    }

    public static String format(OffsetDateTime v) {
        return WRITE_FORMAT.format(v);
    }

}