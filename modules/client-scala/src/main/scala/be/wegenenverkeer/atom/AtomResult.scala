package be.wegenenverkeer.atom

import scala.util.{Failure, Success, Try}

trait AtomResult[+E] {

  def lastSuccessfulEntry:Option[Entry[E]]

  /** Converts an AtomResult to a `Try[Option[Entry[E]]]`
    *
    * If an [[AtomFailure]], returns a [[Failure]] containing is throwable.
    *
    * If an [[AtomSuccess]], returns a [[Success]] containing a [[Some]] with the last successful entry.
    *
    * If an [[AtomNothing]], returns a [[Success]] containing a [[None]]
    * (i.e.: there is no last successful entry).
    */
  def asTry : Try[Option[Entry[E]]] = {
    this match {
      case AtomFailure(_, throwable) => Failure(throwable)
      case other => Success(other.lastSuccessfulEntry)
    }
  }

}

case object AtomNothing extends AtomResult[Nothing] {
  def lastSuccessfulEntry = None
}

case class AtomSuccess[+E](entry:Entry[E]) extends AtomResult[E] {
  def lastSuccessfulEntry:Option[Entry[E]] = Option(entry)
}

case class AtomFailure[+E](lastSuccessfulEntry:Option[Entry[E]], throwable:Throwable) extends AtomResult[E] {
}