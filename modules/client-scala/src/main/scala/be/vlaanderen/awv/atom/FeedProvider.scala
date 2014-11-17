package be.vlaanderen.awv.atom

import com.typesafe.scalalogging.slf4j.Logging
import resource.Resource
import scala.language.implicitConversions
import scala.util.Try

trait FeedProvider[T]  {
  def initialPosition: Option[FeedPosition]
  def fetchFeed() : Try[Feed[T]]
  def fetchFeed(page:String) : Try[Feed[T]]
  def start() : Unit
  def stop() : Unit
}


object FeedProvider extends Logging {
  implicit def managedFeedProvider[T](provider : FeedProvider[T]) : Resource[FeedProvider[T]] = new Resource[FeedProvider[T]] {
    override def open(r: FeedProvider[T]): Unit = {
      logger.debug(s"Opening ${r.getClass.getSimpleName} ... ")
      provider.start()
    }
    override def close(r: FeedProvider[T]): Unit = {
      logger.debug(s"Closing ${r.getClass.getSimpleName} ...")
      provider.stop()
    }
  }
}