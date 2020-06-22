package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;

public interface RxHttpRequestStrategy {
    void apply(ClientRequestBuilder builder);
}
