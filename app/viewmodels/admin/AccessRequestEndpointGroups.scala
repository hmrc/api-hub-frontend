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

package viewmodels.admin

import models.accessrequest.{AccessRequest, AccessRequestEndpoint}

case class AccessRequestEndpointGroup(index: Int, scopes: Set[String], endpoints: Seq[AccessRequestEndpoint])

object AccessRequestEndpointGroups {

  def group(accessRequest: AccessRequest): Seq[AccessRequestEndpointGroup] = {
    accessRequest
      .endpoints
      .map(endpoint => (endpoint.scopes.toSet, endpoint))
      .groupMap(_._1)(_._2)
      .toSeq
      .zipWithIndex
      .map {
        case (group, index) => AccessRequestEndpointGroup(index, group._1, group._2)
      }
  }

}
