package be.wegenenverkeer.atomium.japi.client;

import be.wegenenverkeer.rxhttpclient.HttpClientError;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Created by Karel Maesen, Geovise BVBA on 20/08/15.
 */
public class FailureTest {

    private final static ClasspathFileSource WIREMOCK_MAPPINGS = new ClasspathFileSource("no-self-link-scenario");

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(
            wireMockConfig()
                    .fileSource(WIREMOCK_MAPPINGS)
                    .notifier(new Slf4jNotifier(true))
    );

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    AtomiumClient client;

    @Before
    public void before() {
        client = new AtomiumClient.Builder()
                .setBaseUrl("http://localhost:8080/")
                .setAcceptJson()
                .build();

        //reset WireMock so it will serve the events feed
        WireMock.resetToDefault();
    }

    @After
    public void after() {
        client.close();
    }

    @Test
    public void testReceivingAnError() {
        client.feed("/noselflinkfeed", Event.class).observeFromNowOn(100)
                .take(10)
                .test()
                .awaitDone(5, TimeUnit.SECONDS)
                .assertError(IllegalStateException.class);
    }

}
