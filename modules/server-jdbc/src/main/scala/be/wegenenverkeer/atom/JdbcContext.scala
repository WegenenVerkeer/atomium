package be.wegenenverkeer.atom

import _root_.java.sql.Connection

case class JdbcContext(connection: Connection) extends Context
