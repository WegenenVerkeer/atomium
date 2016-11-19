package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class EntryReference extends FeedPageReference {

    final private int entryNum;

    public EntryReference(int size, int pageNum, int entryNum) {
        super(size, pageNum);
        this.entryNum = entryNum;
    }

    public int getEntryNum(){
        return this.entryNum;
    }

}
