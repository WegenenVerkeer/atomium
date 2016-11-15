package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.format.JacksonJSONCodec;
import be.wegenenverkeer.atomium.format.OffsetDateTimeModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
@Component
public class RestJsonMapper extends JacksonJSONCodec<TestFeedEntryTo> {

    private static final Logger LOG = LoggerFactory.getLogger(RestJsonMapper.class);

    public RestJsonMapper(){
        super(TestFeedEntryTo.class);
    }

}
