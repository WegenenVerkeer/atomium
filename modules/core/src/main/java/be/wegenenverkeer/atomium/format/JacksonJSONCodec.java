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
public class JacksonJSONCodec<T> extends JacksonCodec<FeedPage<T>> implements FeedPageCodec<T,String>   {


    public JacksonJSONCodec(Class<T> entryTypeMarker){
        super();
        this.javaType = this.mapper.getTypeFactory().constructParametricType(FeedPage.class, entryTypeMarker);
    }

}
