package be.wegenenverkeer.atomium.client.rxhttpclient;

import be.wegenenverkeer.rxhttpclient.ClientRequestBuilder;
import io.reactivex.rxjava3.core.Single;

public interface ClientRequestCustomizer {
    Single<ClientRequestBuilder> apply(ClientRequestBuilder builder);
}
