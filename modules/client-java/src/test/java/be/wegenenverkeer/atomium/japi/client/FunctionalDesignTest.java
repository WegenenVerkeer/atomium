package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.atomium.japi.format.Entry;
import be.wegenenverkeer.atomium.japi.format.Feed;
import org.junit.Test;
import rx.Observable;
import rx.Scheduler;

import javax.xml.bind.annotation.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Karel Maesen, Geovise BVBA on 16/03/15.
 */
public class FunctionalDesignTest{

    //TODO -- use a decent mock implementation, for now we just run the Play demo server

    @Test
    public void test() {


        AtomiumClient client = new AtomiumClient.Builder().setBaseUrl("http://localhost:9000/feeds").setAcceptJson()
                .build();


        Observable<Entry<Event>> observable = client.feed("events", Event.class).observe();



//        Observable<Entry<Event>> observable = client.feed("events", Event.class).observeFrom("urn:uuid:adfd1bf9-5456-416c-a811-8490f7f8ed18"
//        , "/events/30/forward/10");
//
        AtomicInteger cnt = new AtomicInteger(0);

        observable.take(100).toBlocking().forEach(entry -> System.out.println(cnt.incrementAndGet() + " > " + entry.getId() + " - " +
                entry.getUpdated() + " links: "));



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
