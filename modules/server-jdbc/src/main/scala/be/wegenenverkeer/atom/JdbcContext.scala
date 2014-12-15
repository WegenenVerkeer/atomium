package be.wegenenverkeer.atom

trait JdbcContext extends Context {

  def session: scala.slick.jdbc.JdbcBackend#SessionDef

}
