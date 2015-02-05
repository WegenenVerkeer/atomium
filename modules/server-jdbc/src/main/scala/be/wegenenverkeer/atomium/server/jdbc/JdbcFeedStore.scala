package be.wegenenverkeer.atomium.server.jdbc

import be.wegenenverkeer.atomium.format.Url

abstract class JdbcFeedStore[E](feedName: String,
                                title: Option[String],
                                entryTableNm: String,
                                ser: E => String,
                                deser: String => E,
                                url: Url)(implicit context: JdbcContext)
  extends AbstractJdbcFeedStore[E](feedName, title, ser, deser, url) {

  self: Dialect =>

}
