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

package models

import models.api.{ApiDetail, EndpointMethod}
import pages.AddAnApiSelectEndpointsPage

case class AvailableEndpoint(path: String, endpointMethod: EndpointMethod)

object AvailableEndpoints {

  def apply(apiDetail: ApiDetail): Map[Set[String], Seq[AvailableEndpoint]] = {
    apiDetail
      .endpoints
      .flatMap(
        endpoint =>
          endpoint.methods.map(
            endpointMethod =>
              (AvailableEndpoint(endpoint.path, endpointMethod), endpointMethod.scopes.toSet)
          )
      )
      .groupMap(_._2)(_._1)
  }

  def selectedEndpoints(apiDetail: ApiDetail, selectedScopes: Set[Set[String]]): Map[Set[String], Seq[AvailableEndpoint]] = {
    AvailableEndpoints(apiDetail)
      .filter(row => selectedScopes.contains(row._1))
  }

  def selectedEndpoints(apiDetail: ApiDetail, userAnswers: UserAnswers): Map[Set[String], Seq[AvailableEndpoint]] = {
    selectedEndpoints(
      apiDetail,
      userAnswers.get(AddAnApiSelectEndpointsPage).getOrElse(Set.empty)
    )
  }

}