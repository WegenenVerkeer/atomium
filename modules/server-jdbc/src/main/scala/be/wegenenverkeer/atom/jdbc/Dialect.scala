package be.wegenenverkeer.atom.jdbc

import java.sql._

import be.wegenenverkeer.atom.JdbcContext
import org.joda.time.LocalDateTime

trait Dialect {

  /**
   * Create an empty feed table.
   *
   * @param jdbcContext The JDBC context to use.
   */
  def createFeedTable(implicit jdbcContext: JdbcContext): Unit

  /**
   * Drop the feed table.
   */
  def dropFeedTable(implicit jdbcContext: JdbcContext): Unit

  /**
   * Create an empty feed entry table with a given name.
   *
   * @param entryTableName The entry table name.
   * @param jdbcContext The JDBC context to use.
   */
  def createEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit

  /**
   * Drop the entry table.
   */
  def dropEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit

  /**
   * Fetch entries from an entry table starting at a given index and entry count.
   *
   * @param entryTableName The name of the entry table.
   * @param start The index of the first entry to fetch.
   * @param count The max number of entries to fetch.
   * @param ascending The direction of the search: ascending or descending indexes.
   * @param jdbcContext The JDBC context to use.
   * @return An entry list.
   */
  def fetchFeedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryData]

  /**
   * Fetch the most recent feed entries from an entry table.
   *
   * @param entryTableName The name of the entry table.
   * @param count The max number of entries to fetch.
   * @param jdbcContext The JDBC context to use.
   * @return An entry list.
   */
  def fetchMostRecentFeedEntries(entryTableName: String, count: Int)(implicit jdbcContext: JdbcContext): List[EntryData]

  /**
   * Add an entry to an entry table.
   *
   * @param entryTableName The name of the entry table.
   * @param entryData The entry to add.
   * @param jdbcContext The JDBC context to use.
   */
  def addFeedEntry(entryTableName: String, entryData: EntryData)(implicit jdbcContext: JdbcContext): Unit

  /**
   * Fetch te largest entry id from an entry table.
   *
   * @param entryTableName The name of the entry table.
   * @param jdbcContext The JDBC context to use.
   * @return The largest entry id for a given entry table.
   */
  def fetchMaxEntryId(entryTableName: String)(implicit jdbcContext: JdbcContext): Long

  /**
   * Fetch the number of entries with an id lower than a given sequence number.
   *
   * @param entryTableName The name of the entry table.
   * @param sequenceNr
   * @param inclusive Indicates whether the given sequence number should be included in the entry count or not.
   * @param jdbcContext The JDBC context to use.
   * @return The number of entries with an id lower than a given sequence number.
   */
  def fetchEntryCountLowerThan(entryTableName: String, sequenceNr: Long, inclusive: Boolean)(implicit jdbcContext: JdbcContext): Long

  /**
   * Helper function to execute an SQL update statement.
   *
   * @param sql The SQL update.
   * @param jdbcContext The JDBC context to use.
   * @return The number of rows affected by the update.
   */
  protected def sqlUpdate(sql: String)(implicit jdbcContext: JdbcContext): Int = {
    val statement = jdbcContext.connection.createStatement()
    statement.executeUpdate(sql)
  }

  /**
   * Helper function to execute a prepared SQL update statement.
   *
   * @param sql The prepared SQL update statement containing ? placeholders where data has to be filled in.
   * @param data The data to fill into the ? placeholders of the prepared SQL update statement.
   * @param jdbcContext The JDBC context to use.
   * @return The number of rows affected by the update.
   */
  protected def sqlUpdatePepared(sql: String, data: Any*)(implicit jdbcContext: JdbcContext): Int = {
    val preparedStatement = jdbcContext.connection.prepareStatement(sql)

    def setPreparedData(remainingData: List[Any], index: Int = 1): Unit = {
      remainingData match {
        case d :: ds =>
          d match {
            case stringData: String => preparedStatement.setString(index, stringData)
            case intData: Int => preparedStatement.setInt(index, intData)
            case timeData: LocalDateTime => preparedStatement.setTimestamp(index, new Timestamp(timeData.toDate.getTime))
            case _ => throw new SQLDataException("Unknown data type used in the atomium feed generator.")
          }
          setPreparedData(ds, index + 1)
        case Nil =>
      }
    }

    setPreparedData(data.toList)
    preparedStatement.executeUpdate()
  }

  /**
   * Helper function to execute an SQL query.
   *
   * @param sql The SQL query to execute.
   * @param maxRows The maxumum number of rows to return (only the first maxRows rows are returned).
   * @param jdbcContext The JDBC context to use.
   * @return The resultset resulting from executing the query.
   */
  protected def sqlQuery[T](sql: String, maxRows: Option[Int], factory: ResultSet => T)(implicit jdbcContext: JdbcContext): List[T] = {

    def processResultSet[T](resultSet: ResultSet, factory: ResultSet => T): List[T] = {
      def processResults: List[T] = {
        resultSet.next() match {
          case false => Nil
          case true => factory(resultSet) :: processResults
        }
      }
      processResults
    }

    val statement = jdbcContext.connection.createStatement()
    maxRows match {
      case Some(rows) => statement.setMaxRows(rows);
      case _ =>
    }
    statement.executeQuery(sql)
    processResultSet[T](statement.getResultSet, factory)
  }

}
