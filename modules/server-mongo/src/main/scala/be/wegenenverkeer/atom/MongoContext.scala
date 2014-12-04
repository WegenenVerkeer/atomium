package be.wegenenverkeer.atom

import com.mongodb.DB

case class MongoContext(db: DB) extends Context
