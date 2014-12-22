package be.wegenenverkeer.atom.jdbc

import be.wegenenverkeer.atom.JdbcContext

trait PostgresDialect extends Dialect {

  override def createFeedTableIfNotExists(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE IF NOT EXISTS ${FeedModel.Table.name} (
         |${FeedModel.Table.idColumn} SERIAL primary key,
         |${FeedModel.Table.nameColumn} varchar NOT NULL,
         |${FeedModel.Table.titleColumn} varchar,
         |UNIQUE(${FeedModel.Table.nameColumn}));""".stripMargin)
  }

  override def dropFeedTable(implicit jdbcContext: JdbcContext): Unit = {
    sqlUpdate(s"DROP TABLE ${FeedModel.Table.name};")
  }

  override def fetchFeed(feedName: String)(implicit jdbcContext: JdbcContext): Option[FeedModel] = {
    val feeds = sqlQuery(
      s"""SELECT * FROM ${FeedModel.Table.name}
         | WHERE ${FeedModel.Table.nameColumn} = '$feedName';
       """.stripMargin, None, FeedModel.apply)
    feeds match {
      case f :: fs => Some(f)
      case Nil => None
    }
  }

  override def addFeed(feed: FeedModel)(implicit jdbcContext: JdbcContext): Unit = {
    val titleData = feed.title match {
      case Some(t) => t
      case None => null
    }
    sqlUpdatePepared(
      s"""INSERT INTO ${FeedModel.Table.name} (${FeedModel.Table.nameColumn}, ${FeedModel.Table.titleColumn})
         |VALUES (?, ?);
       """.stripMargin, feed.name, titleData)
  }

  override def createEntryTableIfNotExists(entryTableName: String)(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE IF NOT EXISTS $entryTableName (
         |${EntryModel.Table.idColumn} SERIAL primary key,
         |${EntryModel.Table.uuidColumn} varchar,
         |${EntryModel.Table.valueColumn} text,
         |${EntryModel.Table.timestampColumn} timestamp not null);""".stripMargin)
  }

  override def dropEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit = {
    sqlUpdate(s"DROP TABLE $entryTableName")
  }

  override def fetchFeedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryModel] = {
    val (comparator, direction) = if (ascending) (">=", "ASC") else ("<=", "DESC")
    sqlQuery(
      s"""SELECT * FROM $entryTableName
         |WHERE ${EntryModel.Table.idColumn} $comparator $start ORDER BY ${EntryModel.Table.idColumn} $direction;
       """.stripMargin,
      Some(count),
      EntryModel.apply
    )
  }

  override def fetchMostRecentFeedEntries(entryTableName: String, count: Int)(implicit jdbcContext: JdbcContext): List[EntryModel] = {
    sqlQuery(
      s"""SELECT * FROM $entryTableName
         |ORDER BY ${EntryModel.Table.idColumn} DESC;
       """.stripMargin,
      Some(count),
      EntryModel.apply
    )
  }

  override def addFeedEntry(entryTableName: String, entryData: EntryModel)(implicit jdbcContext: JdbcContext): Unit = {
    val preparedSql =
      s"""INSERT INTO $entryTableName (${EntryModel.Table.uuidColumn}, ${EntryModel.Table.valueColumn}, ${EntryModel.Table.timestampColumn})
         |VALUES (?,?,?);
       """.stripMargin

    sqlUpdatePepared(preparedSql, entryData.uuid, entryData.value, entryData.timestamp)
  }

  override def fetchMaxEntryId(entryTableName: String)(implicit jdbcContext: JdbcContext): Long = {
    val maxList: List[Long] = sqlQuery[Long](
      s"SELECT max(${EntryModel.Table.idColumn}) as max FROM $entryTableName;",
      None,
      _.getLong("max")
    )
    maxList match {
      case m :: ms => m
      case Nil => 0
    }
  }

  override def fetchEntryCountLowerThan(entryTableName: String, sequenceNr: Long, inclusive: Boolean)(implicit jdbcContext: JdbcContext): Long = {
    val comparator = if (inclusive) "<=" else "<"
    val countList: List[Long] =
      sqlQuery[Long](
        s"""SELECT count(*) as total FROM $entryTableName
         |WHERE ${EntryModel.Table.idColumn} $comparator $sequenceNr;
       """.stripMargin,
        None,
        _.getLong("total")
      )
    countList match {
      case c :: cs => c
      case Nil => 0
    }
  }

}
