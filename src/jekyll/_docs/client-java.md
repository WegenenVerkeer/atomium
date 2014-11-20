---
layout: docs
title: Java
page_title: Client - Java
prev_section: client-general
next_section: client-scala
permalink: /docs/client-java/
---

Client library for handling atomium feeds (which are atom-like feeds in JSON).

See tests for use.

## Add dependency

### Maven

{% highlight xml %}
<dependency>
    <groupId>be.vlaanderen.awv</groupId>
    <artifactId>atomium-client-java</artifactId>
    <version>{{site.version}}</version>
</dependency>
{% endhighlight %}

### SBT

{% highlight scala %}
libraryDependencies += "be.vlaanderen.awv" % "atomium-client-java" % "{{site.version}}"
{% endhighlight %}


## Transaction handling

Not shown here, but you may need to be careful about transaction handling in the entry consumer.

Sample code using a transaction wrapper:

{% highlight java %}
public class MyEntryConsumer<T> implements EntryConsumer<T> {

    private static final Logger LOG = LoggerFactory.getLogger(MyEntryConsumer.class);

    private ConsumerSeparateTxWrapper<T> txWrapper;
    private ParameterService parameterService;
    private String parameterKey;
    private Consumer<T> eventHandler;

    /**
     * Constructor.
     *
     * @param txWrapper service voor het wrappen van de eventHandler in een aparte transactie
     * @param parameterService service voor het opslaan van de parameter
     * @param parameterKey sleutel voor parameter tabel, voor opslaan positie
     * @param eventHandler event handler voor de eigenlijke verwerking van het event
     */
    public DcEntryConsumer(ConsumerSeparateTxWrapper<T> txWrapper, ParameterService parameterService,
            String parameterKey, Consumer<T> eventHandler) {
        this.txWrapper = txWrapper;
        this.parameterService = parameterService;
        this.parameterKey = parameterKey;
        this.eventHandler = eventHandler;
    }

    @Override
    public Validation<FeedProcessingError, FeedPosition> consume(FeedPosition position, Entry<T> entry) {
        try {
            LOG.debug("Process feed entry {} {}.", position, entry);

            txWrapper.accept(eventHandler, entry.content().value().head());
            persistPosition(position);

            return Validation.success(position);
        } catch (Exception e) {
            LOG.error("Fout bij verwerken entry op positie " + position +
                    ", gepropageerd zonder stacktrace: " + e.getMessage(), e);
            return Validation.fail(new FeedProcessingError(new Some(position), e.getMessage()));
        }
    }

    /**
     * Onthouden van de plaats verwerkte entry in de event feed.
     *
     * @param position plaats in de feed
     * @throws ServiceException probleem bij opslaan
     */
    public void persistPosition(FeedPosition position) throws ServiceException {
        parameterService.set(parameterKey, position.link().href().path() + '|' + position.index());
    }
}
{% endhighlight %}

This uses the following transaction wrapper class.

{% highlight java %}
/**
 * Wrapper die een consumer in een aparte transactie uitvoer en eventueel fouten logt en terugdraait.
 *
 * @param <T> consumer parameter type
 */
public interface ConsumerSeparateTxWrapper<T> {

    /**
     * Delegate consumer uitvoeren binnen een aparte transactie.
     * Indien er een probleem is tijdens de uitvoering dan wordt de transactie terug gedraait en de fout wordt gelogd.
     *
     * @param delegate delegate consumer
     * @param parameter parameter
     */
    void accept(Consumer<T> delegate, T parameter);

}
{% endhighlight %}

With this implementation.

{% highlight java %}
/**
 * Wrapper die een consumer in een aparte transactie uitvoer en eventueel fouten logt en terugdraait.
 *
 * @param <T> consumer parameter type
 */
@Component
public class ConsumerSeparateTxWrapperImpl<T> implements ConsumerSeparateTxWrapper<T> {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumerSeparateTxWrapperImpl.class);

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void accept(Consumer<T> delegate, T parameter) {
        try {
            delegate.accept(parameter);
        } catch (Throwable throwable) {
            LOG.error("Exception in consumer, preventing rollback in caller.", throwable);
            try {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            } catch (Throwable te) {
                LOG.error("Transaction could not be set to rollback only.", te);
            }
        }
    }
}
{% endhighlight %}

This combinations assures that any failures in the processing of individual events does are logged but do not block/rollback the saving of the feed position. This means that event which cannot be processed are not retried (you
may need alternate handling for that) and do not block processing of later events.
