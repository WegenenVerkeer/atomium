package be.vlaanderen.awv.atom

import org.joda.time.{DateTime, DateTimeUtils}
import org.scalatest._

class FeedServiceTest extends FunSuite with Matchers with BeforeAndAfterAll {

  val timeMillis = System.currentTimeMillis()
  DateTimeUtils.setCurrentMillisFixed(timeMillis)

  override protected def afterAll(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  val entriesPerPage = 2
  val title = "Test"

  case class Scenario(
    description: String,
    feedInfo: Option[FeedInfo],
    elementsToPush: List[Int],
    resultingUpdates: List[FeedUpdateInfo[Int]],
    resultingInfo: FeedInfo
  )

  def createFeedUpdateInfo(
    page: Long,
    isNew: Boolean,
    newElements: List[Int],
    first: Long,
    previous: Option[Long],
    next: Option[Long]
  ) = {
    FeedUpdateInfo(
      page = page,
      title = title,
      updated = new DateTime(),
      isNew = isNew,
      newElements = newElements,
      first = first,
      previous = previous,
      next = next
    )
  }

  val scenarios = List(
    Scenario(
      description = "er is nog geen page aangemaakt en er wordt een lege lijst van elementen gepushed",
      feedInfo = None,
      elementsToPush = Nil,
      resultingUpdates = List(
        createFeedUpdateInfo(
          page = 1,
          isNew = true,
          newElements = Nil,
          first = 1,
          previous = None,
          next = None
        )
      ),
      resultingInfo = FeedInfo(
        count = 0,
        lastPage = 1
      )
    ),
    Scenario(
      description = "er is al 1 page aangemaakt met 1 element en er wordt een lege lijst van elementen gepushed",
      feedInfo = Some(FeedInfo(1, 1)),
      elementsToPush = Nil,
      resultingUpdates = Nil,
      resultingInfo = FeedInfo(
        count = 1,
        lastPage = 1
      )
    ),
    Scenario(
      description = "er is al 1 page aangemaakt met 2 elementen en er wordt een lege lijst van elementen gepushed",
      feedInfo = Some(FeedInfo(2, 1)),
      elementsToPush = Nil,
      resultingUpdates = Nil,
      resultingInfo = FeedInfo(
        count = 2,
        lastPage = 1
      )
    ),
    Scenario(
      description = "er is nog geen page aangemaakt en er wordt een lijst met 1 element gepushed",
      feedInfo = None,
      elementsToPush = List(1),
      resultingUpdates = List(
        createFeedUpdateInfo(
          page = 1,
          isNew = true,
          newElements = List(1),
          first = 1,
          previous = None,
          next = None
        )
      ),
      resultingInfo = FeedInfo(
        count = 1,
        lastPage = 1
      )
    ),
    Scenario(
      description = "er is al 1 page aangemaakt met 1 element en er wordt een lijst met 1 element gepushed",
      feedInfo = Some(FeedInfo(1, 1)),
      elementsToPush = List(1),
      resultingUpdates = List(
        createFeedUpdateInfo(
          page = 1,
          isNew = false,
          newElements = List(1),
          first = 1,
          previous = None,
          next = None
        )
      ),
      resultingInfo = FeedInfo(
        count = 2,
        lastPage = 1
      )
    ),
    Scenario(
      description = "er is al 1 page aangemaakt met 1 element en er wordt een lijst met 2 elementen gepushed",
      feedInfo = Some(FeedInfo(1, 1)),
      elementsToPush = List(1, 2),
      resultingUpdates = List(
        createFeedUpdateInfo(
          page = 1,
          isNew = false,
          newElements = List(1),
          first = 1,
          previous = None,
          next = Some(2)
        ),
        createFeedUpdateInfo(
          page = 2,
          isNew = true,
          newElements = List(2),
          first = 1,
          previous = Some(1),
          next = None
        )
      ),
      resultingInfo = FeedInfo(
        count = 1,
        lastPage = 2
      )
    ),
    Scenario(
      description = "er is al 1 page aangemaakt met 2 elementen en er wordt een lijst met 1 element gepushed",
      feedInfo = Some(FeedInfo(2, 1)),
      elementsToPush = List(1),
      resultingUpdates = List(
        createFeedUpdateInfo(
          page = 1,
          isNew = false,
          newElements = Nil,
          first = 1,
          previous = None,
          next = Some(2)
        ),
        createFeedUpdateInfo(
          page = 2,
          isNew = true,
          newElements = List(1),
          first = 1,
          previous = Some(1),
          next = None
        )
      ),
      resultingInfo = FeedInfo(
        count = 1,
        lastPage = 2
      )
    )
  )

  scenarios foreach { scenario =>
    test(scenario.description) {
      val (updates, tr) = FeedService.determineFeedUpdates(scenario.elementsToPush, title, entriesPerPage, scenario.feedInfo)
      updates should be (scenario.resultingUpdates)
      tr should be (scenario.resultingInfo)
    }
  }

}
