package be.vlaanderen.awv.atom.javaapi;

import be.vlaanderen.awv.atom.Feed;
import be.vlaanderen.awv.atom.FeedPosition;
import scala.Function0;
import scala.runtime.BoxedUnit;
import scalaz.NonEmptyList;
import scalaz.Validation;

class FeedProcessor<E> {

    private final be.vlaanderen.awv.atom.FeedProcessor feedProcessorScala;

    FeedProcessor(FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        this(null, feedProvider, entryConsumer);
    }

    FeedProcessor(FeedPosition feedPosition, FeedProvider<E> feedProvider, EntryConsumer<E> entryConsumer) {
        feedProcessorScala = new be.vlaanderen.awv.atom.FeedProcessor<E>(
            feedPosition,
            new FeedProviderWrapper<E>(feedProvider),
            new EntryConsumerWrapper<E>(entryConsumer)
        );
    }

    public void start() {
//        Validation<NonEmptyList<String>, BoxedUnit> result = feedProcessorScala.start();
//        if (result.isFailure()) {
//            String msg = result.swap().getOrElse(new Function0<String>(){
//                @Override
//                public String apply() {
//                    return "";
//                }
//            });
//            throw new FeedProcessingException(msg)
//        }
    }


}