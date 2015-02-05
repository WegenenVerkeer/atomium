package be.wegenenverkeer.atomium.server.jdbc

import be.wegenenverkeer.atomium.server.UrlBuilder

abstract class JdbcFeedStore[E](feedName: String,
                                title: Option[String],
                                entryTableNm: String,
                                ser: E => String,
                                deser: String => E,
                                urlBuilder: UrlBuilder)(implicit context: JdbcContext)
  extends AbstractJdbcFeedStore[E](feedName, title, ser, deser, urlBuilder) {

  self: Dialect =>

}
