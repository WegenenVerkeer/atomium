package be.wegenenverkeer.atomium

import be.wegenenverkeer.atomium.format.FeedPage

import scala.util.Try

package object client {

  type FeedEntryUnmarshaller[E] = (String) => Try[FeedPage[E]]

}
