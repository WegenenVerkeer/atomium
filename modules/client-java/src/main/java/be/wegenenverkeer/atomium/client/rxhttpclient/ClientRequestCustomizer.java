package be.wegenenverkeer.atomium.client.rxhttpclient;

import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;

public interface ClientRequestCustomizer {
    void apply(ClientRequestBuilder builder);
}
