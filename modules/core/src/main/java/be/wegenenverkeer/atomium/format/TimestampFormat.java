package be.wegenenverkeer.atomium.format;

import java.time.OffsetDateTime;

/**
 * Created by Karel Maesen, Geovise BVBA on 18/11/16.
 */
public class TimestampFormat {

    public static java.time.format.DateTimeFormatter WRITE_FORMAT =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                    .appendPattern("XXXXX")
                    .toFormatter();

    public static java.time.format.DateTimeFormatter PARSE_FOPRMAT =
            new java.time.format.DateTimeFormatterBuilder()
                    .append(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]"))
                    .optionalStart()
                    .appendPattern("XXXX")
                    .optionalEnd()
                    .optionalStart()
                    .appendPattern("XXX")
                    .optionalEnd()
                    .toFormatter();


    public static OffsetDateTime parse(String v)  {
        return OffsetDateTime.parse(v, PARSE_FOPRMAT);
    }

    public static String format(OffsetDateTime v) {
        return WRITE_FORMAT.format(v);
    }

}
