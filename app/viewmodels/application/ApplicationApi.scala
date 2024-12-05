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

import models.accessrequest.{AccessRequest, Approved}
import models.api.ApiDetailLenses.ApiDetailLensOps
import models.api.{ApiDetail, EndpointMethod}
import models.application.{Api, SelectedEndpoint}
import models.{Enumerable, WithName}
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

  def production(
    applicationScopes: ApplicationScopes,
    pendingAccessRequestCount: Int,
    endpointMethod: EndpointMethod,
  ): ApplicationEndpointAccess = {
    apply(applicationScopes.allowedScopes, pendingAccessRequestCount, endpointMethod)
  }

  def nonProduction(
    applicationScopes: ApplicationScopes,
    pendingAccessRequestCount: Int,
    endpointMethod: EndpointMethod,
  ): ApplicationEndpointAccess = {
    apply(applicationScopes.requiredScopes, pendingAccessRequestCount, endpointMethod)
  }

  private def apply(
    scopes: Set[String],
    pendingAccessRequestCount: Int,
    endpointMethod: EndpointMethod
  ): ApplicationEndpointAccess = {
    if (endpointMethod.scopes.toSet.subsetOf(scopes)) {
      Accessible
    }
    else if (pendingAccessRequestCount > 0) {
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
  productionAccess: ApplicationEndpointAccess,
  nonProductionAccess: ApplicationEndpointAccess
)

object ApplicationEndpoint {

  def forMissingApi(selectedEndpoint: SelectedEndpoint): ApplicationEndpoint = {
    ApplicationEndpoint(
      httpMethod = selectedEndpoint.httpMethod,
      path = selectedEndpoint.path,
      summary = None,
      description = None,
      scopes = Seq.empty,
      productionAccess = Unknown,
      nonProductionAccess = Unknown
    )
  }

  implicit val formatApplicationEndpoint: Format[ApplicationEndpoint] = Json.format[ApplicationEndpoint]

}

case class ApplicationApi(
  apiId: String,
  apiTitle: String,
  totalEndpoints: Int,
  endpoints: Seq[ApplicationEndpoint],
  pendingAccessRequestCount: Int,
  isMissing: Boolean
) {

  def selectedEndpoints: Int = endpoints.size
  def availableProductionEndpoints: Int = endpoints.count(_.productionAccess.isAccessible)
  def availableNonProductionEndpoints: Int = endpoints.count(_.nonProductionAccess.isAccessible)
  def needsProductionAccessRequest: Boolean = !hasPendingAccessRequest && endpoints.exists(_.productionAccess == Inaccessible)
  def hasPendingAccessRequest: Boolean = pendingAccessRequestCount > 0

}

object ApplicationApi {

  def apply(apiDetail: ApiDetail, endpoints: Seq[ApplicationEndpoint], pendingAccessRequestCount: Int): ApplicationApi = {
    ApplicationApi(
      apiId = apiDetail.id,
      apiTitle = apiDetail.title,
      totalEndpoints = apiDetail.endpoints.flatMap(_.methods).size,
      endpoints = endpoints,
      pendingAccessRequestCount = pendingAccessRequestCount,
      isMissing = false
    )
  }

  def apply(api: Api, pendingAccessRequestCount: Int): ApplicationApi = {
    ApplicationApi(
      apiId = api.id,
      apiTitle = api.title,
      totalEndpoints = 0,
      endpoints = api.endpoints.map(ApplicationEndpoint.forMissingApi),
      pendingAccessRequestCount = pendingAccessRequestCount,
      isMissing = true
    )
  }

  implicit val formatApplicationApi: Format[ApplicationApi] = Json.format[ApplicationApi]

}

case class ApplicationScopes(requiredScopes: Set[String], approvedScopes: Set[String]) {
  def allowedScopes: Set[String] = {
    requiredScopes.intersect(approvedScopes)
  }
}

object ApplicationScopes {

  def apply(apis: Seq[(Api, Option[ApiDetail])], accessRequests: Seq[AccessRequest]): ApplicationScopes = {
    ApplicationScopes(
      requiredScopes = buildRequiredScopes(apis),
      approvedScopes = buildApprovedScopes(accessRequests)
    )
  }

  private def buildRequiredScopes(apis: Seq[(Api, Option[ApiDetail])]): Set[String] = {
    apis
      .flatMap(
        api =>
          api._2
            .map(_.getRequiredScopeNames)
            .getOrElse(Set.empty)
      )
      .toSet
  }

  private def buildApprovedScopes(accessRequests: Seq[AccessRequest]): Set[String] = {
    accessRequests
      .filter(_.status == Approved)
      .flatMap(_.endpoints.flatMap(_.scopes))
      .toSet
  }

}
