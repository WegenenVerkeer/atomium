package be.wegenenverkeer.atomium.format;

import be.wegenenverkeer.atomium.api.FeedPage;
import be.wegenenverkeer.atomium.api.FeedPageCodec;

/**
 * Created by Karel Maesen, Geovise BVBA on 15/11/16.
 */
public class JacksonFeedPageCodec<T> extends JacksonCodec<FeedPage<T>> implements FeedPageCodec<T,String>   {


    public JacksonFeedPageCodec(Class<T> entryTypeMarker){
        super();
        this.javaType = this.mapper.getTypeFactory().constructParametricType(FeedPage.class, entryTypeMarker);
    }

}
