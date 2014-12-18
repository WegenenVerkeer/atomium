package be.wegenenverkeer.atom.jdbc

import java.sql.ResultSet

import be.wegenenverkeer.atom.JdbcContext

trait Dialect {

  /**
   * Create an empty feed table.
   *
   * @param jdbcContext The JDBC context to use.
   */
  def createFeedTable(implicit jdbcContext: JdbcContext): Unit

  /**
   * Create an empty feed entry table with a given name.
   *
   * @param entryTableName The entry table name.
   * @param jdbcContext The JDBC context to use.
   */
  def createEntryTable(entryTableName: String)(implicit jdbcContext: JdbcContext): Unit

  /**
   * Fetch entries from an entry table starting at a given index and entry count.
   *
   * @param entryTableName The name of the entry table.
   * @param start The index of the first entry to fetch.
   * @param count The max number of entries to fetch.
   * @param ascending The direction of the search: ascending or descending indexes.
   * @return An entry list.
   */
  def feedEntries(entryTableName: String, start: Long, count: Int, ascending: Boolean)(implicit jdbcContext: JdbcContext): List[EntryData]

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
