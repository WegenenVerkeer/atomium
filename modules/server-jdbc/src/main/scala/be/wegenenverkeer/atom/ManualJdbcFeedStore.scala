package be.wegenenverkeer.atom

import be.wegenenverkeer.atom.jdbc.Dialect

abstract class ManualJdbcFeedStore[E](context: JdbcContext,
                                      feedName: String,
                                      title: Option[String],
                                      entryTableNm: String,
                                      ser: E => String,
                                      deser: String => E,
                                      urlBuilder: UrlBuilder) extends AbstractJdbcFeedStore[E](context, feedName, title, ser, deser, urlBuilder) {

  self: Dialect =>

}
