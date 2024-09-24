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

import models.{Enumerable, WithName}
import models.api.{ApiDetail, EndpointMethod}
import models.application.{Api, Application, EnvironmentName, Primary, Secondary, SelectedEndpoint}
import models.application.ApplicationLenses.ApplicationLensOps
import play.api.libs.json.{Format, Json, Writes}

sealed trait ApplicationEndpointAccess {
  def isAccessible: Boolean
}

case object Accessible extends WithName("accessible") with ApplicationEndpointAccess {
  override val isAccessible: Boolean = true
}

case object Inaccessible extends WithName("inaccessible") with ApplicationEndpointAccess {
  override val isAccessible: Boolean = false
}

case object Requested extends WithName("requested") with ApplicationEndpointAccess {
  override val isAccessible: Boolean = false
}

case object Unknown extends WithName("unknown") with ApplicationEndpointAccess {
  override val isAccessible: Boolean = false
}

object ApplicationEndpointAccess extends Enumerable.Implicits{

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

  val values: Seq[ApplicationEndpointAccess] = Seq(
    Accessible,
    Inaccessible,
    Requested,
    Unknown
  )

  implicit val enumerable: Enumerable[ApplicationEndpointAccess] =
    Enumerable(values.map(v => v.toString -> v)*)

}

case class ApplicationEndpoint(
  httpMethod: String,
  path: String,
  summary: Option[String],
  description: Option[String],
  scopes: Seq[String],
  primaryAccess: ApplicationEndpointAccess,
  secondaryAccess: ApplicationEndpointAccess
)

object ApplicationEndpoint {

  def forMissingApi(selectedEndpoint: SelectedEndpoint): ApplicationEndpoint = {
    ApplicationEndpoint(
      httpMethod = selectedEndpoint.httpMethod,
      path = selectedEndpoint.path,
      summary = None,
      description = None,
      scopes = Seq.empty,
      primaryAccess = Unknown,
      secondaryAccess = Unknown
    )
  }

  implicit val formatApplicationEndpoint: Format[ApplicationEndpoint] = Json.format[ApplicationEndpoint]

}

case class ApplicationApi(
  apiId: String,
  apiTitle: String,
  totalEndpoints: Int,
  endpoints: Seq[ApplicationEndpoint],
  hasPendingAccessRequest: Boolean,
  isMissing: Boolean
) {

  def selectedEndpoints: Int = endpoints.size
  def availablePrimaryEndpoints: Int = endpoints.count(_.primaryAccess.isAccessible)
  def availableSecondaryEndpoints: Int = endpoints.count(_.secondaryAccess.isAccessible)
  def needsProductionAccessRequest: Boolean = !hasPendingAccessRequest && endpoints.exists(_.primaryAccess == Inaccessible)

}

object ApplicationApi {

  def apply(apiDetail: ApiDetail, endpoints: Seq[ApplicationEndpoint], hasPendingAccessRequest: Boolean): ApplicationApi = {
    ApplicationApi(
      apiId = apiDetail.id,
      apiTitle = apiDetail.title,
      totalEndpoints = apiDetail.endpoints.flatMap(_.methods).size,
      endpoints = endpoints,
      hasPendingAccessRequest = hasPendingAccessRequest,
      isMissing = false
    )
  }

  def apply(api: Api, hasPendingAccessRequest: Boolean): ApplicationApi = {
    ApplicationApi(
      apiId = api.id,
      apiTitle = api.title,
      totalEndpoints = 0,
      endpoints = api.endpoints.map(ApplicationEndpoint.forMissingApi),
      hasPendingAccessRequest = hasPendingAccessRequest,
      isMissing = true
    )
  }

  implicit val formatApplicationApi: Format[ApplicationApi] = Json.format[ApplicationApi]

}
