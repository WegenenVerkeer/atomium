package be.wegenenverkeer.atomium.client;

public class FeedPosition {
    String pageUrl;
    String entryId;

    public FeedPosition(String pageUrl, String entryId) {
        this.pageUrl = pageUrl;
        this.entryId = entryId;
    }

    public FeedPosition(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public String getEntryId() {
        return entryId;
    }
}

