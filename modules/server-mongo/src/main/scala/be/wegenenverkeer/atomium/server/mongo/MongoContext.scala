package be.wegenenverkeer.atomium.server.mongo

import be.wegenenverkeer.atomium.server.Context
import com.mongodb.DB

case class MongoContext(db: DB) extends Context
