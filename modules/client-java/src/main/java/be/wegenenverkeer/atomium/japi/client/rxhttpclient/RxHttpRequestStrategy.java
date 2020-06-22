package be.wegenenverkeer.atomium.japi.client.rxhttpclient;

import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;

public interface RxHttpRequestStrategy {
    void apply(ClientRequestBuilder builder);
}
