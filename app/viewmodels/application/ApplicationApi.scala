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

package viewmodels.application

import models.api.ApiDetail

sealed trait ApplicationEndpointAccess

case object Accessible extends ApplicationEndpointAccess
case object Inaccessible extends ApplicationEndpointAccess
case object Requested extends ApplicationEndpointAccess

case class ApplicationEndpoint(
  httpMethod: String,
  path: String,
  scopes: Seq[String],
  primaryAccess: ApplicationEndpointAccess,
  secondaryAccess: ApplicationEndpointAccess
)

case class ApplicationApi(apiDetail: ApiDetail, endpoints: Seq[ApplicationEndpoint]) {

  def selectedEndpoints: Int = endpoints.size
  def totalEndpoints: Int = apiDetail.endpoints.flatMap(_.methods).size
  def availablePrimaryEndpoints: Int = endpoints.count(_.primaryAccess == Accessible)
  def availableSecondaryEndpoints: Int = endpoints.count(_.secondaryAccess == Accessible)

}
