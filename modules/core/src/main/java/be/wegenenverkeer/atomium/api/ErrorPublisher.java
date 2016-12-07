package be.wegenenverkeer.atomium.api;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by Karel Maesen, Geovise BVBA on 07/12/16.
 */
class ErrorPublisher<T> implements Publisher<FeedPage<T>> {

    final Throwable throwable;


    public ErrorPublisher(Throwable trowable) {
        this.throwable = trowable;
    }

    @Override
    public void subscribe(Subscriber<? super FeedPage<T>> s) {
        s.onSubscribe( new InnerSubscription<>(s, throwable));
    }


}

class InnerSubscription<K> implements Subscription {

    final Subscriber<K> sub;
    final Throwable throwable;

    InnerSubscription(Subscriber<K> sub, Throwable t) {
        this.sub = sub;
        this.throwable = t;
    }

    @Override
    public void request(long n) {
        this.sub.onError(throwable);
    }

    /**
     * Request the {@link Publisher} to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel as this request is asynchronous.
     */
    @Override
    public void cancel() {
        //not necessary
    }
}