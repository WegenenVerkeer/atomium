package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.AtomiumDecodeException;
import be.wegenenverkeer.atomium.api.AtomiumEncodeException;
import be.wegenenverkeer.atomium.api.Codec;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.TimeZone;

/**
 * Created by Karel Maesen, Geovise BVBA on 13/12/16.
 */
public class JacksonCodec<T> implements Codec<T, String> {

    final protected ObjectMapper mapper;
    protected JavaType javaType;

    public JacksonCodec(Class<T> entryTypeMarker){
        this();
        this.javaType = this.mapper.getTypeFactory().constructType(entryTypeMarker);
    }

    protected JacksonCodec() {
        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m.setTimeZone(TimeZone.getDefault()); //this is required since default TimeZone is GMT in Jackson!
        m.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        m.registerModule(new OffsetDateTimeModule());
        this.mapper = m;
    }

    public void registerModules(Module... modules) {
        this.mapper.registerModules(modules);
    }

    @Override
    public String getMimeType() {
        return "application/json";
    }

    @Override
    public String encode(T value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new AtomiumEncodeException(e.getMessage(),e);
        }
    }

    @Override
    public T decode(String encoded) {
        try {
            return mapper.readValue(encoded, javaType);
        } catch(Exception e) {
            throw new AtomiumDecodeException(e.getMessage(), e);
        }
    }
}
