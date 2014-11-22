package controllers

import javax.xml.bind.annotation.{XmlAccessType, XmlAccessorType, XmlRootElement}

import be.vlaanderen.awv.jaxb.{xmlElement, xmlAttribute, StringOptionAdapter, xmlTypeAdapter}
import play.api.libs.functional.syntax._
import play.api.libs.json._

object EventFormat {

  implicit def eventWrites: Writes[Event] = (
    (__ \ "value").write[Double] and
      (__ \ "description").writeNullable[String] and
      (__ \ "version").write[Int]
    )(in => (in.value, in.description, in.version))

  implicit def eventReads: Reads[Event] = (
    (__ \ "value").read[Double] and
      (__ \ "description").readNullable[String] and
      (__ \ "version").read[Int]
    )((value, description, version) => Event(value, description, version))

}

@XmlRootElement
@XmlAccessorType (XmlAccessType.NONE)
case class Event(@xmlElement value: Double,
                 @xmlElement @xmlTypeAdapter(classOf[StringOptionAdapter]) description: Option[String],
                 @xmlAttribute version: Int = 1) {
    def this() = this(0.0, None, 0)

}
