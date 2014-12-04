package be.wegenenverkeer.atom.java

import com.mongodb.DBObject

trait ElementMapper[E] {
  def serialize(e: E): DBObject
  def deserialize(dbo: DBObject): E
}
