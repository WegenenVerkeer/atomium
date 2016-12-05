package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class EntryPointer extends FeedPageReference {

    final private int entryNum;

    public EntryPointer(int pageNum, int entryNum) {
        super(pageNum);
        this.entryNum = entryNum;
    }

    public int getEntryNum(){
        return this.entryNum;
    }

}
