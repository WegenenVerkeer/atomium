package be.wegenenverkeer.atomium.server.slick

import be.wegenenverkeer.atomium.server.Context

trait SlickJdbcContext extends Context {

  def session: scala.slick.jdbc.JdbcBackend#SessionDef

}
