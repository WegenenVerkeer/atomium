package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class FeedPageReference {

    final private int pageNum;

    public FeedPageReference(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageNum(){
        return this.pageNum;
    }


}
