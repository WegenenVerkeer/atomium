package be.vlaanderen.awv.atom

import com.mongodb.DB

case class MongoContext(db: DB) extends Context
