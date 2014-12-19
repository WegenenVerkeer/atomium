package be.wegenenverkeer.atom.jdbc

import be.wegenenverkeer.atom.JdbcContext

trait PostgresDialect extends Dialect {

  override def createFeedTable(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE ${FeedData.Table.name} (
         |${FeedData.Table.idColumn} SERIAL primary key,
         |${FeedData.Table.nameColumn} varchar not NULL,
         |${FeedData.Table.titleColumn} varchar);""".stripMargin)
  }

  override def dropFeedTable(implicit jdbcContext: JdbcContext): Unit = {
    sqlUpdate(s"DROP TABLE ${FeedData.Table.name}")
  }

  override def createEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE $entryTableName (
         |${EntryData.Table.idColumn} SERIAL primary key,
         |${EntryData.Table.uuidColumn} varchar,
         |${EntryData.Table.valueColumn} text,
         |${EntryData.Table.timestampColumn} timestamp not null);""".stripMargin)
  }

  override def dropEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit = {
    sqlUpdate(s"DROP TABLE $entryTableName")
  }

  override def fetchFeedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryData] = {
    val (comparator, direction) = if(ascending) (">=", "ASC") else ("<=", "DESC")
    sqlQuery(
      s"""SELECT * FROM $entryTableName
         |WHERE ${EntryData.Table.idColumn} $comparator $start ORDER BY ${EntryData.Table.idColumn} $direction;
       """.stripMargin,
      Some(count),
      EntryData.apply
    )
  }

  override def fetchMostRecentFeedEntries(entryTableName: String, count: Int)(implicit jdbcContext: JdbcContext): List[EntryData] = {
    sqlQuery(
      s"""SELECT * FROM $entryTableName
         |ORDER BY ${EntryData.Table.idColumn} DESC;
       """.stripMargin,
      Some(count),
      EntryData.apply
    )
  }

  override def addFeedEntry(entryTableName: String, entryData: EntryData)(implicit jdbcContext: JdbcContext): Unit = {
    val preparedSql =
      s"""INSERT INTO $entryTableName (${EntryData.Table.uuidColumn}, ${EntryData.Table.valueColumn}, ${EntryData.Table.timestampColumn})
         |VALUES (?,?,?)
       """.stripMargin

    sqlUpdatePepared(preparedSql, entryData.uuid, entryData.value, entryData.timestamp)
  }

  override def fetchMaxEntryId(entryTableName: String)(implicit jdbcContext: JdbcContext): Long = {
    val maxList: List[Long] = sqlQuery[Long](
      s"SELECT max(${EntryData.Table.idColumn}) as max FROM $entryTableName;",
      None,
      _.getLong("max")
    )
    maxList match {
      case m :: ms => m
      case Nil => 0
    }
  }

  override def fetchEntryCountLowerThan(entryTableName: String, sequenceNr: Long, inclusive: Boolean)(implicit jdbcContext: JdbcContext): Long = {
    val comparator = if(inclusive) "<=" else "<"
    val countList: List[Long] =
      sqlQuery[Long](
        s"""SELECT count(*) as total FROM $entryTableName
         |WHERE ${EntryData.Table.idColumn} $comparator $sequenceNr;
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
