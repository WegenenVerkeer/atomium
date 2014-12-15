package be.wegenenverkeer.atom.slick

import java.sql.Connection

import be.wegenenverkeer.atom.JdbcContext

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.UnmanagedSession

/**
 * The slice of the cake which provides the Slick driver
 */
trait DriverComponent {
  val driver: JdbcProfile

  import driver.simple._

  def createJdbcContext(implicit session: Session) = DriverBoundJdbcContext(session)

  case class DriverBoundJdbcContext(session: Session) extends JdbcContext

  object DriverBoundJdbcContext {

    implicit def session2Context(session: Session): DriverBoundJdbcContext =
      DriverBoundJdbcContext(session)

    implicit def connection2Context(connection: Connection): DriverBoundJdbcContext =
      DriverBoundJdbcContext(new UnmanagedSession(connection))
  }


}