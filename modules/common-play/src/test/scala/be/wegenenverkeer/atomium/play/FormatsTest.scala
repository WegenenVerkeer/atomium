package be.wegenenverkeer.atomium.play

import java.time.{OffsetDateTime, ZoneOffset}

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.{JsString, Json}
import PlayJsonFormats._
import be.wegenenverkeer.atomium.format.Adapters

class FormatsTest extends FunSuite with Matchers {


  test("Marshal exactly like java formats") {
    val datetime = OffsetDateTime.now();
    val expected = new Adapters.AtomDateTimeAdapter().marshal(datetime)

    dateTimeFormat.writes(datetime) shouldBe JsString(expected)
  }


  test("Marshal UTC values exactly like java formats"){
    val datetime = OffsetDateTime.of(2016,8,22,1,20,3,0, ZoneOffset.ofHours(0));
    val expected = new Adapters.AtomDateTimeAdapter().marshal(datetime)
    dateTimeFormat.writes(datetime) shouldBe JsString(expected)
  }

  test("Unmarshal exactly like java formats") {
    val datetime = OffsetDateTime.now();
    val jsVal = PlayJsonFormats.dateTimeFormat.writes(datetime)
    val expected = new Adapters.AtomDateTimeAdapter().unmarshal(jsVal.as[String])
    jsVal.as[OffsetDateTime] shouldBe expected
  }

  test("Unmarshal UTC exactly like java formats") {
    val datetime = OffsetDateTime.of(2016,8,22,1,20,3,0, ZoneOffset.ofHours(0));
    val jsVal = PlayJsonFormats.dateTimeFormat.writes(datetime)
    jsVal.as[OffsetDateTime] shouldBe datetime
  }
}
