package be.wegenenverkeer.atomium.play

import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import jakarta.xml.bind.annotation.{ XmlAttribute, XmlElement, XmlElementRef, XmlValue }

import scala.annotation.meta.field

package object jaxb {

  type xmlElement = XmlElement @field
  type xmlElementRef = XmlElementRef @field
  type xmlAttribute = XmlAttribute @field
  type xmlValue = XmlValue @field
  type xmlTypeAdapter = XmlJavaTypeAdapter @field

}
