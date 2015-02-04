package be.wegenenverkeer.atomium.server.jdbc

import be.wegenenverkeer.atomium.server.UrlBuilder

abstract class JdbcFeedStore[E](context: JdbcContext,
                                      feedName: String,
                                      title: Option[String],
                                      entryTableNm: String,
                                      ser: E => String,
                                      deser: String => E,
                                      urlBuilder: UrlBuilder)
  extends AbstractJdbcFeedStore[E](context, feedName, title, ser, deser, urlBuilder) {

  self: Dialect =>

}
