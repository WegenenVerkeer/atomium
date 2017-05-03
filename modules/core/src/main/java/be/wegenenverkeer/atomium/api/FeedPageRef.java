package be.wegenenverkeer.atomium.api;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/11/16.
 */
public class FeedPageRef implements Comparable<FeedPageRef> {

    final private long pageNum;

    public FeedPageRef(long pageNum) {
        this.pageNum = pageNum;
    }

    public long getPageNum(){
        return this.pageNum;
    }

    public static FeedPageRef page(long pageNum) {
        return new FeedPageRef(pageNum);
    }

    public static FeedPageRef forLastPage(){
        return new FeedPageRef(0);
    }

    public boolean isStrictlyMoreRecentThan(FeedPageRef o) {
        return this.compareTo(o) > 0;
    }

    public boolean isStrictlyOlderThan(FeedPageRef o){
        return this.compareTo(o) < 0;
    }

    @Override
    public int compareTo(FeedPageRef o) {
        return Long.compare(this.pageNum, o.pageNum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FeedPageRef that = (FeedPageRef) o;

        return pageNum == that.pageNum;

    }

    @Override
    public int hashCode() {
        return (int) (pageNum ^ (pageNum >>> 32));
    }
}
