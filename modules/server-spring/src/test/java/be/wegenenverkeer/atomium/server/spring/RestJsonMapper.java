package be.wegenenverkeer.atomium.server.spring;

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
public class RestJsonMapper extends ObjectMapper {

    private static final Logger LOG = LoggerFactory.getLogger(RestJsonMapper.class);


    /**
     * No-arguments constructor.
     */
    public RestJsonMapper() {
        super();

        this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.setTimeZone(TimeZone.getDefault()); //this is required since default TimeZone is GMT in Jackson!
        this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        this.registerModule(new OffsetDateTimeModule());
    }



}
