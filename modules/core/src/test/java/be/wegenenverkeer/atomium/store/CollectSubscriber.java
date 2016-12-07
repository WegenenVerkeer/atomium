package be.wegenenverkeer.atomium.store;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karel Maesen, Geovise BVBA on 05/12/16.
 */
public class CollectSubscriber<T> implements Subscriber<T> {

    public Subscription subscription;
    public List<T> received = new ArrayList<>();
    public boolean isCompleted = false;

    @Override
    public void onSubscribe(Subscription s) {
        subscription = s;
    }

    /**
     * Data notification sent by the {@link Publisher} in response to requests to {@link Subscription#request(long)}.
     *
     * @param t the element signaled
     */
    @Override
    public void onNext(T t) {
        received.add(t);
    }

    /**
     * Failed terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     *
     * @param t the throwable signaled
     */
    @Override
    public void onError(Throwable t) {
        throw new RuntimeException(t);
    }

    /**
     * Successful terminal state.
     * <p>
     * No further events will be sent even if {@link Subscription#request(long)} is invoked again.
     */
    @Override
    public void onComplete() {
        isCompleted = true;
    }

    public void request(int n){
        this.subscription.request(n);
    }

}
