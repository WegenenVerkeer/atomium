package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.format.Entry;
import be.wegenenverkeer.atomium.japi.format.Feed;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

import javax.xml.bind.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class FunctionalDesignTest{

    //TODO -- use a decent mock implementation, for now we just run the Play demo server

    @Test
    public void test() throws InterruptedException {


        AtomiumClient client = new AtomiumClient.Builder()
                .setBaseUrl("http://localhost:9000")
                .setAcceptXml()
                .build();

        Observable<Entry<Event>> observable = client.feed("feeds/events", Event.class).observe(1000);


        AtomicInteger cnt = new AtomicInteger(0);

        observable.take(1000).toBlocking().forEach(entry -> System.out.println(cnt.incrementAndGet() + " > " + entry.getId() + " - " +
                entry.getUpdated() +  " - " + entry.getContent().getValue()));


        client.close();
    }

}





@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
class Event {

    @XmlElement
    public Double value;

    @XmlElement
    public String description;

    @XmlAttribute
    public Integer version;

    public Event(){
    }


    public String toString() {
        return "Event " + version + " " + "description " + " value: " + value;
    }

}
