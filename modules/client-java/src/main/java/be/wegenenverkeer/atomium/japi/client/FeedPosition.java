package be.wegenenverkeer.atomium.japi.client;

import io.reactivex.rxjava3.core.Single;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class FeedPosition {
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

