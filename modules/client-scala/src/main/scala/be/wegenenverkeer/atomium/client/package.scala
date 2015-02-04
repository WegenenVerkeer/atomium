package be.wegenenverkeer.atomium

import be.wegenenverkeer.atomium.format.Feed

import scala.util.Try

package object client {

  type FeedEntryUnmarshaller[E] = (String) => Try[Feed[E]]

}
