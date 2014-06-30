package be.vlaanderen.awv.atom.slick

import com.github.tminglei.slickpg._
import scala.slick.driver.PostgresDriver

trait SlickPostgresDriver extends PostgresDriver
with PgArraySupport
with FixedPgDateSupportJoda
with PgRangeSupport
with PgHStoreSupport
with PgSearchSupport {
  /// for formats support
  type DOCType = text.Document

  ///
  override val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus {}

  //////
  trait ImplicitsPlus extends Implicits
  with ArrayImplicits
  with DateTimeImplicits
  with RangeImplicits
  with HStoreImplicits
  with SearchImplicits

  trait SimpleQLPlus extends SimpleQL
  with ImplicitsPlus
  with SearchAssistants
}

object SlickPostgresDriver extends SlickPostgresDriver