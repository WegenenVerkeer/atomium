package be.wegenenverkeer.atom.jdbc

import be.wegenenverkeer.atom.JdbcContext

object PostgresDialect extends Dialect {

  override def createFeedTable(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE ${FeedData.Table.name} (
         |${FeedData.Table.idColumn} SERIAL primary key,
         |${FeedData.Table.nameColumn} varchar not NULL,
         |${FeedData.Table.titleColumn} varchar);""".stripMargin)
  }

  override def createEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext) = {
    sqlUpdate(
      s"""CREATE TABLE $entryTableName (
         |${EntryData.Table.idColumn} SERIAL primary key,
         |${EntryData.Table.uuidColumn} varchar,
         |${EntryData.Table.valueColumn} text,
         |${EntryData.Table.timestampColumn} timestamp not null);""".stripMargin)
  }

  override def feedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryData] = {

    if (ascending) {
      sqlQuery(
        s"""SELECT * FROM $entryTableName
         |WHERE ${EntryData.Table.idColumn} >= $start ORDER BY ${EntryData.Table.idColumn} ASC;
       """.stripMargin,
        Some(count),
        EntryData.apply
      )
    } else {
      sqlQuery(
        s"""SELECT * FROM $entryTableName
         |WHERE ${EntryData.Table.idColumn} <= $start ORDER BY ${EntryData.Table.idColumn} DESC;
       """.stripMargin,
        Some(count),
        EntryData.apply
      )
    }

  }

}