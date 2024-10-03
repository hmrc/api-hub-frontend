/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.accessrequest

import play.api.libs.json.{Format, Json}

import java.time.LocalDateTime
import scala.math.Ordered.orderingToOrdered

case class AccessRequest(
  id: String,
  applicationId: String,
  apiId: String,
  apiName: String,
  status: AccessRequestStatus,
  endpoints: Seq[AccessRequestEndpoint],
  supportingInformation: String,
  requested: LocalDateTime,
  requestedBy: String,
  decision: Option[AccessRequestDecision],
  cancelled: Option[AccessRequestCancelled]
)

object AccessRequest {

  def apply(
    id: String,
    applicationId: String,
    apiId: String,
    apiName: String,
    status: AccessRequestStatus,
    supportingInformation: String,
    requested: LocalDateTime,
    requestedBy: String,
  ): AccessRequest = {
    AccessRequest(
      id = id,
      applicationId = applicationId,
      apiId = apiId,
      apiName = apiName,
      status = status,
      endpoints = Seq.empty,
      supportingInformation = supportingInformation,
      requested = requested,
      requestedBy = requestedBy,
      decision = None,
      cancelled = None
    )
  }

  implicit val formatAccessRequest: Format[AccessRequest] = Json.format[AccessRequest]

  implicit val accessRequestOrdering: Ordering[AccessRequest] = (x: AccessRequest, y: AccessRequest) => {
    (y.requested, x.applicationId, x.apiName) compare (x.requested, y.applicationId, y.apiName)
  }

}
