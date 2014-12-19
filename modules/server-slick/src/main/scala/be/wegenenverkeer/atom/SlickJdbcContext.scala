package be.wegenenverkeer.atom

trait SlickJdbcContext extends Context {

  def session: scala.slick.jdbc.JdbcBackend#SessionDef

}
