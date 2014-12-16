package be.wegenenverkeer.atom.java

import be.wegenenverkeer.atom.slick.FeedComponent
import be.wegenenverkeer.atom.{AbstractFeedStore, JdbcContext, UrlBuilder}

class ManualJdbcFeedStore[E](feedComponent: FeedComponent,
                       context: JdbcContext,
                       feedName: String,
                       title: String,
                       feedEntriesTableName: String,
                       mapper: ElementMapper[E],
                       urlProvider: UrlBuilder) extends FeedStore[E](feedName, Option(title), urlProvider) {

  override def underlying: AbstractFeedStore[E] =
    be.wegenenverkeer.atom.ManualJdbcFeedStore[E](
      feedComponent = feedComponent,
      context = context,
      feedName = feedName,
      title = Option(title),
      feedEntriesTableName,
      ser = (e) => mapper.serialize(e),
      deser = (v) => mapper.deserialize(v),
      urlBuilder = urlProvider
    )
}
