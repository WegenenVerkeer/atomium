package be.wegenenverkeer.atom.java;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class Adapters {

    public static DateTimeFormatter outputFormatterWithSecondsAndOptionalTZ = new DateTimeFormatterBuilder()
            .append(ISODateTimeFormat.dateHourMinuteSecond())
            .appendTimeZoneOffset("Z", true, 2, 4)
            .toFormatter();

    public static class AtomDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

        @Override
        public LocalDateTime unmarshal(String v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.parseDateTime(v).toLocalDateTime();
        }

        @Override
        public String marshal(LocalDateTime v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.print(v.toDateTime());
        }
    }

}
