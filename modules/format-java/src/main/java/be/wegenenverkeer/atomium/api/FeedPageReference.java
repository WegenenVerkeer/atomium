package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class FeedPageReference {

    final private int size;
    final private int pageNum;

    public FeedPageReference(int size, int pageNum) {
        this.size = size;
        this.pageNum = pageNum;
    }

    public int getPageSize(){
        return this.size;
    }

    public int getPageNum(){
        return this.pageNum;
    }


}
