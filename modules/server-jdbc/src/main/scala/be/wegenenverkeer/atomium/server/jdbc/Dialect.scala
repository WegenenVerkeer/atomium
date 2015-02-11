package be.wegenenverkeer.atomium.server.jdbc

import java.sql._

import org.joda.time.DateTime

trait Dialect {


  /**
   * @param entryTableName The entry table name.
   * @return the sql statement for the Feed table.
   */
  protected def createEntryTableStatement(entryTableName: String): String

  /**
   * Create an empty feed entry table with a given name.
   *
   * @param entryTableName The entry table name.
   * @param jdbcContext The JDBC context to use.
   */
  protected def createEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit =
    sqlUpdate(createEntryTableStatement(entryTableName))

  /**
   * Drop the entry table.
   */
  protected def dropEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit


  /**
   * Fetch a feed model based on the feed name.
   * @param feedName The feed name.
   * @param jdbcContext The JDBC context to use.
   * @return A feed model wrapped in an option (None if there is no feed model with the given name).
   */
  def fetchFeed(feedName: String)(implicit jdbcContext: JdbcContext): Option[FeedDbModel]

  /**
   * Add a feed definition to the feed table.
   *
   * @param feed The feed to add.
   * @param jdbcContext The JDBC context to use.
   * @return The id for the newly added feed.
   */
  def addFeed(feed: FeedDbModel)(implicit jdbcContext: JdbcContext): Unit

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
  def fetchFeedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryDbModel]

  /**
   * Fetch the most recent feed entries from an entry table.
   *
   * @param entryTableName The name of the entry table.
   * @param count The max number of entries to fetch.
   * @param jdbcContext The JDBC context to use.
   * @return An entry list.
   */
  def fetchMostRecentFeedEntries(entryTableName: String, count: Int)(implicit jdbcContext: JdbcContext): List[EntryDbModel]

  /**
   * Add an entry to an entry table.
   *
   * @param entryTableName The name of the entry table.
   * @param entryData The entry to add.
   * @param jdbcContext The JDBC context to use.
   */
  def addFeedEntry(entryTableName: String, entryData: EntryDbModel)(implicit jdbcContext: JdbcContext): Unit

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
   * @param sequenceNr the sequence number
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
            case intData: Int       => preparedStatement.setInt(index, intData)
            case timeData: DateTime => preparedStatement.setTimestamp(index, new Timestamp(timeData.toDate.getTime))
            case _                  => throw new SQLDataException("Unknown data type used in the atomium feed generator.")
          }
          setPreparedData(ds, index + 1)
        case Nil     =>
      }
    }

    setPreparedData(data.toList)
    preparedStatement.executeUpdate()
  }

  /**
   * Helper function to execute an SQL query.
   *
   * @param sql The SQL query to execute.
   * @param maxRows The maximum number of rows to return (only the first maxRows rows are returned).
   * @param jdbcContext The JDBC context to use.
   * @return The result set resulting from executing the query.
   */
  protected def sqlQuery[T](sql: String, maxRows: Option[Int], factory: ResultSet => T)(implicit jdbcContext: JdbcContext): List[T] = {

    def processResultSet(resultSet: ResultSet, factory: ResultSet => T): List[T] = {
      def processResults: List[T] = {
        resultSet.next() match {
          case false => Nil
          case true  => factory(resultSet) :: processResults
        }
      }
      processResults
    }

    val statement = jdbcContext.connection.createStatement()
    maxRows.foreach(rows => statement.setMaxRows(rows))

    statement.executeQuery(sql)
    processResultSet(statement.getResultSet, factory)
  }

}
