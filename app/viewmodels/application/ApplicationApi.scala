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

import models.api.{ApiDetail, EndpointMethod}
import models.application.{Application, EnvironmentName, Primary, Secondary}
import models.application.ApplicationLenses.ApplicationLensOps

sealed trait ApplicationEndpointAccess {
  def isAccessible: Boolean
}

case object Accessible extends ApplicationEndpointAccess {
  override val isAccessible: Boolean = true
}

case object Inaccessible extends ApplicationEndpointAccess {
  override val isAccessible: Boolean = false
}

case object Requested extends ApplicationEndpointAccess {
  override val isAccessible: Boolean = false
}

object ApplicationEndpointAccess {

  def apply(
    application: Application,
    hasPendingAccessRequest: Boolean,
    endpointMethod: EndpointMethod,
    environmentName: EnvironmentName
  ): ApplicationEndpointAccess = {

    val scopes = environmentName match {
      case Primary => application.getPrimaryScopes
      case Secondary => application.getSecondaryScopes
    }

    if (endpointMethod.scopes.toSet.subsetOf(scopes.map(_.name).toSet)) {
      Accessible
    }
    else if (hasPendingAccessRequest) {
      Requested
    }
    else {
      Inaccessible
    }

  }

}

case class ApplicationEndpoint(
  httpMethod: String,
  path: String,
  scopes: Seq[String],
  primaryAccess: ApplicationEndpointAccess,
  secondaryAccess: ApplicationEndpointAccess
)

case class ApplicationApi(apiDetail: ApiDetail, endpoints: Seq[ApplicationEndpoint], hasPendingAccessRequest: Boolean) {

  def selectedEndpoints: Int = endpoints.size
  def totalEndpoints: Int = apiDetail.endpoints.flatMap(_.methods).size
  def availablePrimaryEndpoints: Int = endpoints.count(_.primaryAccess.isAccessible)
  def availableSecondaryEndpoints: Int = endpoints.count(_.secondaryAccess.isAccessible)
  def needsProductionAccessRequest: Boolean = !hasPendingAccessRequest && (endpoints.exists(_.primaryAccess == Inaccessible))

}
