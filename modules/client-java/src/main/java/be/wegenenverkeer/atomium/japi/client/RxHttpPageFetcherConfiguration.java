package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttpclient.RxHttpClient;
import com.fasterxml.jackson.databind.Module;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RxHttpPageFetcherConfiguration<E> {
    private RxHttpClient rxHttpClient;
    private Callable<Map<String, String>> extraHeaders;
    private List<Module> modules;
    private Class<E> entryTypeMarker;
    private String feedUrl;

    public RxHttpClient getRxHttpClient() {
        return rxHttpClient;
    }

    public void setRxHttpClient(RxHttpClient rxHttpClient) {
        this.rxHttpClient = rxHttpClient;
    }

    public Callable<Map<String, String>> getExtraHeaders() {
        return extraHeaders;
    }

    public void setExtraHeaders(Callable<Map<String, String>> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    public List<Module> getModules() {
        return modules;
    }

    public void setExtraModules(List<Module> modules) {
        this.modules = modules;
    }

    public Class<E> getEntryTypeMarker() {
        return entryTypeMarker;
    }

    public void setEntryTypeMarker(Class<E> entryTypeMarker) {
        this.entryTypeMarker = entryTypeMarker;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }
}
