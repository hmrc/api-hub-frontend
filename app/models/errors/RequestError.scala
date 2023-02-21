package models.errors

import models.Enumerable
import play.api.libs.json.{Format, Json}

sealed trait RequestErrorCode

case object BadRequest extends RequestErrorCode
case object NotFound extends RequestErrorCode
case object InvalidJson extends RequestErrorCode
case object ApplicationNameNotUnique extends RequestErrorCode

object RequestErrorCode extends Enumerable.Implicits {

  val values: Seq[RequestErrorCode] = Seq(
    InvalidJson
  )

  implicit val enumerable: Enumerable[RequestErrorCode] =
    Enumerable(values.map(value => value.toString -> value): _*)

}

case class RequestError(reason: RequestErrorCode, description: String)

object RequestError {

  implicit val formatRequestError: Format[RequestError] = Json.format[RequestError]

}
