package be.wegenenverkeer.atom.java

trait ElementMapper[E] {
  def serialize(e: E): String
  def deserialize(value: String): E
}
