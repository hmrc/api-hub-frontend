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

import models.Lens

import java.time.LocalDateTime

object AccessRequestLenses {

  val accessRequestEndpoints: Lens[AccessRequest, Seq[AccessRequestEndpoint]] =
    Lens[AccessRequest, Seq[AccessRequestEndpoint]](
      get = _.endpoints,
      set = (accessRequest, endpoints) => accessRequest.copy(endpoints = endpoints)
    )

  val accessRequestDecision: Lens[AccessRequest, Option[AccessRequestDecision]] =
    Lens[AccessRequest, Option[AccessRequestDecision]](
      get = _.decision,
      set = (accessRequest, decision) => accessRequest.copy(decision = decision)
    )

  val accessRequestStatus: Lens[AccessRequest, AccessRequestStatus] =
    Lens[AccessRequest, AccessRequestStatus](
      get = _.status,
      set = (accessRequest, status) => accessRequest.copy(status = status)
    )

  implicit class AccessRequestLensOps(accessRequest: AccessRequest) {

    def setEndpoints(endpoints: Seq[AccessRequestEndpoint]): AccessRequest = {
      accessRequestEndpoints.set(accessRequest, endpoints)
    }

    def addEndpoint(endpoint: AccessRequestEndpoint): AccessRequest = {
      accessRequestEndpoints.set(
        accessRequest,
        accessRequest.endpoints :+ endpoint
      )
    }

    def addEndpoint(httpMethod: String, path: String, scopes: Seq[String]): AccessRequest = {
      addEndpoint(AccessRequestEndpoint(httpMethod, path, scopes))
    }

    def setDecision(decision: Option[AccessRequestDecision]): AccessRequest = {
      accessRequestDecision.set(accessRequest, decision)
    }

    def setDecision(decision: AccessRequestDecision): AccessRequest = {
      setDecision(Some(decision))
    }

    def setDecision(decided: LocalDateTime, decidedBy: String): AccessRequest = {
      setDecision(AccessRequestDecision(decided, decidedBy, None))
    }

    def setDecision(decided: LocalDateTime, decidedBy: String, rejectedReason: String): AccessRequest = {
      setDecision(AccessRequestDecision(decided, decidedBy, Some(rejectedReason)))
    }

    def setStatus(status: AccessRequestStatus): AccessRequest = {
      accessRequestStatus.set(accessRequest, status)
    }

  }

}
