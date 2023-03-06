package org.anized.ingestor

import akka.http.scaladsl.model.{StatusCode, StatusCodes}

package object ServiceErrors {
  class ServiceError(val code: StatusCode, val message: String,
                     cause: Option[Throwable] = None) extends Throwable {
    override def getMessage: String = message
    override def getCause: Throwable = cause.getOrElse(this)
    override def toString: String = s"$code: $message"
  }
  case class InternalError(reason: String) extends ServiceError(StatusCodes.InternalServerError, reason)
  case class NotFound(reason: String) extends ServiceError(StatusCodes.NotFound, reason)
  case class BadRequest(reason: String) extends ServiceError(StatusCodes.BadRequest, reason)

  def mapException(code: StatusCode): Throwable = code match {
    case StatusCodes.NotFound => NotFound(code.value)
    case StatusCodes.BadRequest => BadRequest(code.value)
    case _ => InternalError(code.value)
  }

}