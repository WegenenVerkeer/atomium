package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.AtomiumDecodeException;
import be.wegenenverkeer.atomium.api.AtomiumEncodeException;
import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.TimeZone;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public class JacksonJSONCodec<T> implements FeedPageCodec<T,String> {

    final private ObjectMapper mapper;
    final private JavaType javaType;

    public JacksonJSONCodec(Class<T> entryTypeMarker){
        ObjectMapper m =  new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m.setTimeZone(TimeZone.getDefault()); //this is required since default TimeZone is GMT in Jackson!
        m.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        m.registerModule(new OffsetDateTimeModule());
        this.mapper = m;
        this.javaType = this.mapper.getTypeFactory().constructParametricType(FeedPage.class, entryTypeMarker);
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String encode(FeedPage<T> page) {
        try {
            return mapper.writeValueAsString(page);
        } catch (JsonProcessingException e) {
            throw new AtomiumEncodeException(e.getMessage(),e);
        }
    }

    @Override
    public FeedPage<T> decode(String encoded) {
        try {
            return mapper.readValue(encoded, javaType);
        } catch(Exception e) {
            throw new AtomiumDecodeException(e.getMessage(), e);
        }
    }
}
