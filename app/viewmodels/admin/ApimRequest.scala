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

package viewmodels.admin

import config.HipEnvironment
import connectors.ApimConnector
import models.api.{ApiDeployment, EgressGateway}
import models.application.ClientScope
import models.deployment.{DeploymentDetails, StatusResponse, SuccessfulDeploymentResponse}
import models.exception.ApimException
import play.api.libs.json.{Json, OFormat, Writes}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

sealed trait ApimRequest[T] {

  def url: String
  
  def id: String

  def paramNames: Seq[String] = {
    val regex = """(\{\w+})""".r

    regex.findAllIn(url)
      .toSeq
      .map(_.replace("{", ""))
      .map(_.replace("}", ""))
  }

  def makeRequest(
    apimConnector: ApimConnector,
    hipEnvironment: HipEnvironment,
    paramValues: Seq[String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    if (paramValues.size != paramNames.size) {
      throw new IllegalArgumentException(s"Expected ${paramNames.size} parameters but found ${paramValues.size}")
    }
    else {
      buildRequest(apimConnector, hipEnvironment, paramValues)
        .map(resultToString)
    }
  }

  protected def buildRequest(
    apimConnector: ApimConnector,
    hipEnvironment: HipEnvironment,
    paramValues: Seq[String]
  )(implicit hc: HeaderCarrier): Future[Either[ApimException, T]]

  private def resultToString(result: Either[ApimException, T]): String = {
    result match {
      case Right(s) => Json.prettyPrint(Json.toJson(s)(writes))
      case Left(e) => (s"ApimException: ${e.message}" +: "" +: "Stack trace:" +: e.getStackTrace.map("    " + _.toString)).mkString(System.lineSeparator())
    }
  }

  def writes: Writes[T]
}

object ApimRequests {

  val getDeployments: ApimRequest[Seq[ApiDeployment]] = new ApimRequest[Seq[ApiDeployment]]() {

    override val url: String = "/v1/oas-deployments"
    
    override val id: String = "getDeployments"

    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[ApiDeployment]]] = {
      apimConnector.getDeployments(hipEnvironment)
    }

    override val writes: Writes[Seq[ApiDeployment]] = implicitly

  }

  val getDeployment: ApimRequest[SuccessfulDeploymentResponse] = new ApimRequest[SuccessfulDeploymentResponse] {

    override val url: String = "/v1/oas-deployments/{id}"
    
    override val id: String = "getDeployment"

    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, SuccessfulDeploymentResponse]] = {
      apimConnector.getDeployment(hipEnvironment, paramValues.head)
    }

    override val writes: Writes[SuccessfulDeploymentResponse] = implicitly

  }

  val getOpenApiSpecification: ApimRequest[String] = new ApimRequest[String] {

    override val url: String = "/v1/oas-deployments/{id}/oas"

    override val id: String = "getOpenApiSpecification"
    
    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, String]] = {
      apimConnector.getOpenApiSpecification(hipEnvironment, paramValues.head)
    }

    override val writes: Writes[String] = implicitly
  }

  val getDeploymentDetails: ApimRequest[DeploymentDetails] = new ApimRequest[DeploymentDetails] {

    override val url: String = "/v1/simple-api-deployment/deployments/{serviceId}"

    override val id: String = "getDeploymentDetails"
    
    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, DeploymentDetails]] = {
      apimConnector.getDeploymentDetails(hipEnvironment, paramValues.head)
    }
    override val writes: Writes[DeploymentDetails] = implicitly

  }

  val getDeploymentStatus: ApimRequest[StatusResponse] = new ApimRequest[StatusResponse] {

    override val url: String = "/v1/simple-api-deployment/deployments/{serviceId}/status?mr-iid={mergeRequestIid}&version={version}"

    override val id: String = "getDeploymentStatus"
    
    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, StatusResponse]] = {
      apimConnector.getDeploymentStatus(
        hipEnvironment = hipEnvironment,
        publisherRef = paramValues.head,
        mergeRequestIid = paramValues(1),
        version = paramValues(2)
      )
    }

    override val writes: Writes[StatusResponse] = implicitly
  }

  val listEgressGateways: ApimRequest[Seq[EgressGateway]] = new ApimRequest[Seq[EgressGateway]] {

    override val url: String = "/v1/simple-api-deployment/egress-gateways"

    override val id: String = "listEgressGateways"
    
    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[EgressGateway]]] = {
      apimConnector.listEgressGateways(hipEnvironment)
    }

    override val writes: Writes[Seq[EgressGateway]] = implicitly
  }

  val fetchClientScopes: ApimRequest[Seq[ClientScope]] = new ApimRequest[Seq[ClientScope]] {

    override val url: String = "/identity/clients/{id}/client-scopes"
    
    override val id: String = "fetchClientScopes"

    override protected def buildRequest(
      apimConnector: ApimConnector,
      hipEnvironment: HipEnvironment,
      paramValues: Seq[String]
    )(implicit hc: HeaderCarrier): Future[Either[ApimException, Seq[ClientScope]]] = {
      apimConnector.fetchClientScopes(hipEnvironment, paramValues.head)
    }

    override val writes: Writes[Seq[ClientScope]] = implicitly
  }

  val requests: Seq[ApimRequest[?]] = Seq(
    getDeployments,
    getDeployment,
    getOpenApiSpecification,
    getDeploymentDetails,
    getDeploymentStatus,
    listEgressGateways,
    fetchClientScopes
  )

}
