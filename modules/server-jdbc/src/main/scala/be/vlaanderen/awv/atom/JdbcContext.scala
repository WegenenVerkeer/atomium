package be.vlaanderen.awv.atom

import _root_.java.sql.Connection

import scala.slick.backend.DatabaseComponent
import scala.slick.jdbc.UnmanagedSession

case class JdbcContext(session: DatabaseComponent#SessionDef) extends Context

object JdbcContext {
  implicit def session2Context(session: DatabaseComponent#SessionDef) = JdbcContext(session)
  implicit def connection2Context(connection: Connection) = JdbcContext(new UnmanagedSession(connection))
}
