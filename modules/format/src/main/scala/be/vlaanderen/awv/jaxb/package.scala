package be.vlaanderen.awv

import javax.xml.bind.annotation.adapters.{XmlAdapter, XmlJavaTypeAdapter}
import javax.xml.bind.annotation.{XmlValue, XmlAttribute, XmlElementRef, XmlElement}
import scala.annotation.meta.field

package object jaxb {

  type xmlElement     = XmlElement @field
  type xmlElementRef  = XmlElementRef @field
  type xmlAttribute   = XmlAttribute @field
  type xmlValue       = XmlValue @field
  type xmlTypeAdapter = XmlJavaTypeAdapter @field

  /**
   * Taken from,
   * https://gist.github.com/krasserm/1891525#file-jaxb-02-scala
   */
  class OptionAdapter[A >: Null](nones: A*) extends XmlAdapter[A, Option[A]] {
    def marshal(v: Option[A]): A = {
      v.getOrElse(nones(0))
    }
    def unmarshal(v: A) = {
      if (nones contains v) None else Some(v)
    }
  }

  /**
   * NB The double type parameters ([S,A]) are necessary because MOXy always uses the first one as a target for casting
   * from the input string before passing to unmarshal. Unfortunately this means you need to always pass String as
   * the first type parameter when extending the CustomOptionAdapter (eg see DateTimeOptionAdapter).
   *
   * These bugs describe the behaviour and fix,
   *   - https://bugs.eclipse.org/bugs/show_bug.cgi?id=440681
   *   - https://bugs.eclipse.org/bugs/show_bug.cgi?id=431803
   * The fix should be available in the next release after 2.5.2
   */
  class CustomOptionAdapter[S,A](customAdapter:XmlAdapter[String,A], nones: String*) extends XmlAdapter[String, Option[A]] {
    def marshal(v: Option[A]): String = {
      v.map(customAdapter.marshal).getOrElse(nones(0))
    }
    def unmarshal(v: String):Option[A] = {
      if (nones contains v) None else Some(customAdapter.unmarshal(v))
    }
  }

  /**
   * Taken from,
   * https://gist.github.com/krasserm/1891525#file-jaxb-02-scala
   */
  class StringOptionAdapter extends OptionAdapter[String](null, "")

  class LongAdapter extends XmlAdapter[String, java.lang.Long] {
    override def unmarshal(v: String) = java.lang.Long.parseLong(v)
    override def marshal(v: java.lang.Long) = v.toString
  }

  class LongOptionAdapter extends CustomOptionAdapter[String,java.lang.Long](new LongAdapter,null)

  class DoubleAdapter extends XmlAdapter[String, java.lang.Double] {
    override def unmarshal(v: String) = java.lang.Double.parseDouble(v)
    override def marshal(v: java.lang.Double) = v.toString
  }

  class IntAdapter extends XmlAdapter[String, java.lang.Integer] {
    override def unmarshal(v: String) = java.lang.Integer.parseInt(v)
    override def marshal(v: java.lang.Integer) = v.toString
  }
}
