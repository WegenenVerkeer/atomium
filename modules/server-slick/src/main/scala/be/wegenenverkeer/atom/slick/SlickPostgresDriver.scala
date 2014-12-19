package be.wegenenverkeer.atom.slick

import com.github.tminglei.slickpg._

import scala.slick.driver.PostgresDriver

trait SlickPostgresDriver extends PostgresDriver with PgDateSupportJoda {

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus extends Implicits with DateTimeImplicits

  trait SimpleQLPlus extends SimpleQL with ImplicitsPlus
}

object SlickPostgresDriver extends SlickPostgresDriver