package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.slick.FeedComponent
import be.wegenenverkeer.atom.{SlickJdbcContext, AbstractFeedStore, UrlBuilder}

class AutoSlickFeedStore[E](feedComponent: FeedComponent,
                       context: SlickJdbcContext,
                       feedName: String,
                       title: String,
                       mapper: ElementMapper[E],
                       urlProvider: UrlBuilder) extends FeedStore[E](feedName, Option(title), urlProvider) {

  override def underlying: AbstractFeedStore[E] =
    new be.wegenenverkeer.atom.AutoSlickFeedStore[E](
      feedComponent = feedComponent,
      context = context,
      feedName = feedName,
      title = Option(title),
      ser = (e) => mapper.serialize(e),
      deser = (v) => mapper.deserialize(v),
      urlBuilder = urlProvider
    )
}
