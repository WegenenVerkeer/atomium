package be.vlaanderen.awv.atom.java

import fj.data.{Validation => JavaValidation}

import scalaz.Scalaz._
import scalaz._

object Validations {

  def toJavaValidation[E, T](validation:Validation[E, T]) : JavaValidation[E, T]  = {
    validation match  {
      case Failure(error) => JavaValidation.fail(error)
      case Success(succValue) => JavaValidation.success(succValue)
    }
  }

  def toScalazValidation[E, T](validation:JavaValidation[E, T]) : Validation[E, T] = {
    if (validation.isSuccess) {
      validation.success.success[E]
    } else {
      validation.fail.fail[T]
    }
  }

}
