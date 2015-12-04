package be.wegenenverkeer.atomium.client

import play.api.libs.json.{Json, JsObject, OWrites}

object AtomiumFeedFormats {

  implicit val atomiumFeedClientStatusWrites = new OWrites[AtomiumFeedClientStatus] {

    override def writes(o: AtomiumFeedClientStatus): JsObject = Json.obj(
      "ok" -> o.ok,
      "status" -> o.status,
      "error" -> o.error,
      "feedPositie" -> o.feedPosition,
      "entryId" -> o.entryId,
      "ontvangenOp" -> o.receivedOn
    )
  }

}
