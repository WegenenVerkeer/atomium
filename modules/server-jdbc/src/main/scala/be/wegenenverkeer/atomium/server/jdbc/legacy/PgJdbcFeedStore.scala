package be.wegenenverkeer.atomium.server.jdbc.legacy

import be.wegenenverkeer.atomium.format.Url

case class PgJdbcFeedStore[E](feedName: String,
                              title: Option[String],
                              entryTableName: String,
                              ser: E => String,
                              deser: String => E,
                              url: Url)
  extends AbstractJdbcFeedStore[E](feedName, title, ser, deser, url)
  with PostgresDialect
