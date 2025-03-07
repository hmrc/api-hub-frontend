/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.application

import config.{HipEnvironment,HipEnvironments}
import models.accessrequest.AccessRequest

case class AccessRequestsByEnvironment (accessRequests: Seq[AccessRequest], hipEnvironments: HipEnvironments) {
  def groupByEnvironment: Seq[EnvironmentAccessRequests] = {
    accessRequests.groupBy(_.environmentId).map {
      case (environmentId, accessRequests) => EnvironmentAccessRequests(hipEnvironments.forId(environmentId), accessRequests.toSet)
    }.toSeq.sortBy(_.environment.rank)(Ordering[Int].reverse)
  }
}

case class EnvironmentAccessRequests (environment: HipEnvironment, accessRequests: Set[AccessRequest])
