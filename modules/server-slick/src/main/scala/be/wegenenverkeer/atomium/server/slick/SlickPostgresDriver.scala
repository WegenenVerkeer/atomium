package be.wegenenverkeer.atomium.server.slick

import com.github.tminglei.slickpg._

import scala.slick.driver.PostgresDriver

trait SlickPostgresDriver extends PostgresDriver  {

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  trait ImplicitsPlus extends Implicits

  trait SimpleQLPlus extends SimpleQL with ImplicitsPlus
}

object SlickPostgresDriver extends SlickPostgresDriver