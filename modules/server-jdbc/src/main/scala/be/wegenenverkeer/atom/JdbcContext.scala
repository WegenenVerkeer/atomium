package be.wegenenverkeer.atom

import _root_.java.sql.Connection

trait JdbcContext extends Context{

  def connection: Connection

}
