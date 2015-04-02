package be.wegenenverkeer.atomium.japi.format;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class Adapters {

    public static DateTimeFormatter outputFormatterWithSecondsAndOptionalTZ = new DateTimeFormatterBuilder()
            .append(ISODateTimeFormat.dateHourMinuteSecond())
            .appendTimeZoneOffset("Z", true, 2, 4)
            .toFormatter();

    public static class AtomDateTimeAdapter extends XmlAdapter<String, DateTime> {

        @Override
        public DateTime unmarshal(String v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.parseDateTime(v).toDateTime();
        }

        @Override
        public String marshal(DateTime v) throws Exception {
            return outputFormatterWithSecondsAndOptionalTZ.print(v);
        }
    }

}
