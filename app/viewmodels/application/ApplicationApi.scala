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

import config.HipEnvironment
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
  theoreticalScopes: TheoreticalScopes,
  pendingAccessRequests: Seq[AccessRequest]
) {
  def accessFor(hipEnvironment: HipEnvironment): ApplicationEndpointAccess = {
    val allowedScopes = hipEnvironment.isProductionLike match {
      case true => theoreticalScopes.allowedScopes(hipEnvironment)
      case false => theoreticalScopes.requiredScopes
    }

    if (scopes.isEmpty) {
      Unknown
    } else if (scopes.toSet.subsetOf(allowedScopes)) {
      Accessible
    } else if (pendingAccessRequests.exists(_.environmentId.contains(hipEnvironment.id))) {
      Requested
    } else {
      Inaccessible
    }
  }
}


object ApplicationEndpoint {

  def forMissingApi(selectedEndpoint: SelectedEndpoint): ApplicationEndpoint = {
    ApplicationEndpoint(
      httpMethod = selectedEndpoint.httpMethod,
      path = selectedEndpoint.path,
      summary = None,
      description = None,
      scopes = Seq.empty,
      theoreticalScopes = TheoreticalScopes(Set.empty, Map.empty),
      pendingAccessRequests = Seq.empty
    )
  }

  implicit val formatApplicationEndpoint: Format[ApplicationEndpoint] = Json.format[ApplicationEndpoint]

}

case class ApplicationApi(
  apiId: String,
  apiTitle: String,
  endpoints: Seq[ApplicationEndpoint],
  pendingAccessRequests: Seq[AccessRequest],
  isMissing: Boolean
) {

  def selectedEndpoints: Int = endpoints.size
  def availableEndpoints(hipEnvironment: HipEnvironment): Int = endpoints.count(_.accessFor(hipEnvironment) == Accessible)
  def needsAccessRequest(hipEnvironment: HipEnvironment): Boolean = !hasPendingAccessRequest(hipEnvironment) && endpoints.exists(_.accessFor(hipEnvironment) == Inaccessible)
  def hasPendingAccessRequest(hipEnvironment: HipEnvironment): Boolean = pendingAccessRequests.exists(_.environmentId.contains(hipEnvironment.id))
  def isAccessibleInEnvironment(hipEnvironment: HipEnvironment): Boolean = availableEndpoints(hipEnvironment) > 0
  def allEndpointsHavePendingAccessRequests(hipEnvironment: HipEnvironment): Boolean = {
    endpoints.forall(endpoint => pendingAccessRequests.exists(par => par.environmentId == hipEnvironment.id && par.endpoints.exists(e => e.path == endpoint.path && e.httpMethod == endpoint.httpMethod)))
  }
}

object ApplicationApi {

  def apply(apiDetail: ApiDetail, endpoints: Seq[ApplicationEndpoint], pendingAccessRequests: Seq[AccessRequest]): ApplicationApi = {
    ApplicationApi(
      apiId = apiDetail.id,
      apiTitle = apiDetail.title,
      endpoints = endpoints,
      pendingAccessRequests = pendingAccessRequests,
      isMissing = false
    )
  }

  def apply(api: Api, pendingAccessRequests: Seq[AccessRequest]): ApplicationApi = {
    ApplicationApi(
      apiId = api.id,
      apiTitle = api.title,
      endpoints = api.endpoints.map(ApplicationEndpoint.forMissingApi),
      pendingAccessRequests = pendingAccessRequests,
      isMissing = true
    )
  }

  implicit val formatApplicationApi: Format[ApplicationApi] = Json.format[ApplicationApi]

}

case class TheoreticalScopes(requiredScopes: Set[String], approvedScopes: Map[String, Set[String]]) {
  def allowedScopes(hipEnvironment: HipEnvironment): Set[String] = {
    requiredScopes.intersect(approvedScopes.get(hipEnvironment.id).getOrElse(Set.empty))
  }
  def filterByScopes(scopes: Set[String]): TheoreticalScopes = {
    copy(
      requiredScopes = requiredScopes.intersect(scopes),
      approvedScopes = approvedScopes.view.mapValues(_.intersect(scopes)).filter { case (_, value) => value.nonEmpty }.toMap
    )
  }
}

object TheoreticalScopes {

  def apply(apis: Seq[(Api, Option[ApiDetail])], accessRequests: Seq[AccessRequest]): TheoreticalScopes = {
    TheoreticalScopes(
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

  private def buildApprovedScopes(accessRequests: Seq[AccessRequest]): Map[String,Set[String]] = {
    accessRequests
      .filter(_.status == Approved)
      .groupMapReduce(_.environmentId)(_.endpoints.flatMap(_.scopes).toSet)(_ ++ _)
  }

  implicit val formatTheoreticalScopes: Format[TheoreticalScopes] = Json.format[TheoreticalScopes]
}
