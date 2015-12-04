package be.wegenenverkeer.atomium.client

import scala.concurrent.duration.{Duration, _}


case class FeedConfig(baseUrl: String,
                      feedUrl: String,
                      pollingInterval: Duration = 5000.millis,
                      connectTimeout: Duration = 10000.millis)