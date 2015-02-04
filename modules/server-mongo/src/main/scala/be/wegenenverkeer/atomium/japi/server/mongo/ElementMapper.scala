package be.wegenenverkeer.atomium.japi.server.mongo

import com.mongodb.DBObject

trait ElementMapper[E] {
  def serialize(e: E): DBObject
  def deserialize(dbo: DBObject): E
}
