package be.wegenenverkeer.atomium.format.pub

sealed trait Draft {
  def value:String
}

object Draft {
  def apply(value:String): Draft = if (value == DraftYes.value) DraftYes else DraftNo
}
object DraftYes extends Draft {
  override def value = "yes"
}

object DraftNo extends Draft {

  override def value = "no"
}