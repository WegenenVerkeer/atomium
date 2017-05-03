package be.wegenenverkeer.atomium.server.spring;

import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.format.JacksonFeedPageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
@Component
public class RestJsonMapper extends JacksonFeedPageCodec<TestFeedEntryTo> {

    private static final Logger LOG = LoggerFactory.getLogger(RestJsonMapper.class);

    public RestJsonMapper(){
        super(TestFeedEntryTo.class);
    }

    //so that Mockito works correctly here!!
    @Override
    public String encode(FeedPage<TestFeedEntryTo> value) {
        return super.encode(value);
    }
}
