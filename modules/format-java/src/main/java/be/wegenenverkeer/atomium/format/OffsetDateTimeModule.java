package be.wegenenverkeer.atomium.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.*;

import java.io.IOException;
import java.time.*;

/**
 * This is a custom Jackson module that ensures that the Jackson Serializer/Deserializer follows the same rules as the other JSon
 * support modules
 *
 * Created by Karel Maesen, Geovise BVBA on 23/08/16.
 */
public class OffsetDateTimeModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public OffsetDateTimeModule() {
        super();
        // first deserializer
        addDeserializer(OffsetDateTime.class, DESERIALIZER);
        // then serializers
        addSerializer(OffsetDateTime.class, OffsetDateTimeSerializer.INSTANCE);
    }

    public static final StdScalarDeserializer<OffsetDateTime> DESERIALIZER = new StdScalarDeserializer<OffsetDateTime>(OffsetDateTime.class){


        /* @param jp   Parsed used for reading JSON content
         * @param ctxt Context that can be used to access information about
         *             this deserialization activity.
         * @return Deserialized value
         */
        @Override
        public OffsetDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            switch(jp.getCurrentToken())
            {
                case VALUE_STRING:
                    String string = jp.getText().trim();
                    return OffsetDateTime.parse(string, Adapters.datetimeParser);
            }
            throw ctxt.mappingException("Expected type string.");
        }
    };

}
