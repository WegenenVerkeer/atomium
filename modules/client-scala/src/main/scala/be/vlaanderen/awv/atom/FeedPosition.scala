package be.vlaanderen.awv.atom
/**
 * class be.vlaanderen.awv.atom.FeedPosition
 * @author Peter Rigole
 *         company <a href="http://www.Qmino.com">Qmino</a>
 *         Creation-Date: 9/05/14
 *         Creation-Time: 16:17
 */
case class FeedPosition(page: String, index: Int) {

  def syncReference: String = s"$page${FeedPosition.delimiter}$index"

}

object FeedPosition {
  val delimiter: String = ":"

  def fromSyncRef(syncRef:String) : FeedPosition = {
    val positionValues = syncRef.split(FeedPosition.delimiter)
    FeedPosition(positionValues(0), positionValues(1).toInt)
  }
}