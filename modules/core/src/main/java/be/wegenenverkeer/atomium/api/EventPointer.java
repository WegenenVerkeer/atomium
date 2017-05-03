package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class EventPointer extends FeedPageRef {

    final private int eventNumberInPage;

    public EventPointer(int pageNum, int eventNumberInPage) {
        super(pageNum);
        this.eventNumberInPage = eventNumberInPage;
    }

    public int getEventNumberInPage(){
        return this.eventNumberInPage;
    }

}
