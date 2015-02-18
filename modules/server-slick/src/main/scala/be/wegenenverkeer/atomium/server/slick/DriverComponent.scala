package be.wegenenverkeer.atomium.server.slick

import java.sql.Connection

import scala.slick.driver.JdbcProfile
import scala.slick.jdbc.UnmanagedSession

/**
 * The slice of the cake which provides the Slick driver
 */
trait DriverComponent {
  val driver: JdbcProfile

  import driver.simple._

  def createJdbcContext(implicit session: Session) = DriverBoundSlickJdbcContext(session)

  case class DriverBoundSlickJdbcContext(session: Session) extends SlickJdbcContext

  object DriverBoundSlickJdbcContext {

    implicit def session2Context(session: Session): DriverBoundSlickJdbcContext =
      DriverBoundSlickJdbcContext(session)

    implicit def connection2Context(connection: Connection): DriverBoundSlickJdbcContext =
      DriverBoundSlickJdbcContext(new UnmanagedSession(connection))
  }


}