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
import fakes.FakeHipEnvironments
import models.api.{ApiDeployment, EgressGateway}
import models.application.ClientScope
import models.deployment.{DeploymentDetails, EgressMapping, StatusResponse, SuccessfulDeploymentResponse}
import models.exception.ApimException
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future

class ApimRequestSpec extends AsyncFreeSpec with Matchers with MockitoSugar with TableDrivenPropertyChecks {

  import ApimRequestSpec.*

  "ApimRequest" - {
    "must parse parameter names correctly" in {
      val paramLists = Table(
        ("request", "paramNames"),
        (ApimRequests.getDeployments, Seq.empty),
        (ApimRequests.getDeployment, Seq("id")),
        (ApimRequests.getOpenApiSpecification, Seq("id")),
        (ApimRequests.getDeploymentDetails, Seq("serviceId")),
        (ApimRequests.getDeploymentStatus, Seq("serviceId", "mergeRequestIid", "version")),
        (ApimRequests.listEgressGateways, Seq.empty),
        (ApimRequests.fetchClientScopes, Seq("id"))
      )

      forAll(paramLists) {(request: ApimRequest[?], paramNames: Seq[String]) =>
        request.paramNames mustBe paramNames
      }
    }

    "must reject a request with an invalid number of parameters" in {
      val apimConnector = mock[ApimConnector]

      an [IllegalArgumentException] must be thrownBy {
        ApimRequests.getDeployments.makeRequest(apimConnector, environment, Seq("invalid argument"))
      }
    }

    "must build the result string correctly on success" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getDeployments(any)(any)).thenReturn(Future.successful(Right(deployments)))

      ApimRequests.getDeployments.makeRequest(apimConnector, environment, Seq.empty).map {
        result =>
          result mustBe Json.prettyPrint(Json.toJson(deployments))
      }
    }

    "must build the result string correctly on failure" in {
      val failures = Table(
        "failure",
        ApimException.badGateway(),
        ApimException.unexpectedResponse(500),
        ApimException.serviceNotFound(publisherRef),
        ApimException.credentialNotFound(clientId),
        ApimException.error(new Exception("test-message"))
      )

      val apimConnector = mock[ApimConnector]

      forAll(failures) {(failure: ApimException) =>
        when(apimConnector.getDeployments(any)(any)).thenReturn(Future.successful(Left(failure)))

        ApimRequests.getDeployments.makeRequest(apimConnector, environment, Seq.empty).map {
          result =>
            result mustBe failure.getStackTrace.mkString(System.lineSeparator())
        }
      }
    }
  }

  "getDeployments" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getDeployments(eqTo(environment))(any))
        .thenReturn(Future.successful(Right(deployments)))

      ApimRequests.getDeployments.makeRequest(apimConnector, environment, Seq.empty).map {
        result =>
          result mustBe prettyPrint(deployments)
      }
    }
  }

  "getDeployment" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getDeployment(eqTo(environment), eqTo(publisherRef))(any))
        .thenReturn(Future.successful(Right(deployment)))

      ApimRequests.getDeployment.makeRequest(apimConnector, environment, Seq(publisherRef)).map {
        result =>
          result mustBe prettyPrint(deployment)
      }
    }
  }

  "getOpenApiSpecification" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getOpenApiSpecification(eqTo(environment), eqTo(publisherRef))(any))
        .thenReturn(Future.successful(Right(oas)))

      ApimRequests.getOpenApiSpecification.makeRequest(apimConnector, environment, Seq(publisherRef)).map {
        result =>
          result mustBe prettyPrint(oas)
      }
    }
  }

  "getDeploymentDetails" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getDeploymentDetails(eqTo(environment), eqTo(publisherRef))(any))
        .thenReturn(Future.successful(Right(deploymentDetails)))

      ApimRequests.getDeploymentDetails.makeRequest(apimConnector, environment, Seq(publisherRef)).map {
        result =>
          result mustBe prettyPrint(deploymentDetails)
      }
    }
  }

  "getDeploymentStatus" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.getDeploymentStatus(eqTo(environment), eqTo(publisherRef), eqTo(mergeRequestIid), eqTo(version))(any))
        .thenReturn(Future.successful(Right(statusResponse)))

      ApimRequests.getDeploymentStatus.makeRequest(apimConnector, environment, Seq(publisherRef, mergeRequestIid, version)).map {
        result =>
          result mustBe prettyPrint(statusResponse)
      }
    }
  }

  "listEgressGateways" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.listEgressGateways(eqTo(environment))(any))
        .thenReturn(Future.successful(Right(egressGateways)))

      ApimRequests.listEgressGateways.makeRequest(apimConnector, environment, Seq.empty).map {
        result =>
          result mustBe prettyPrint(egressGateways)
      }
    }
  }

  "fetchClientScopes" - {
    "must make the correct request to APIM" in {
      val apimConnector = mock[ApimConnector]

      when(apimConnector.fetchClientScopes(eqTo(environment), eqTo(clientId))(any))
        .thenReturn(Future.successful(Right(clientScopes)))

      ApimRequests.fetchClientScopes.makeRequest(apimConnector, environment, Seq(clientId)).map {
        result =>
          result mustBe prettyPrint(clientScopes)
      }
    }
  }

}

private object ApimRequestSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val environment: HipEnvironment = FakeHipEnvironments.test
  val publisherRef: String = "test-publisher-ref"
  val clientId: String = "test-client-id"
  val mergeRequestIid: String = "test-merge-request-iid"
  val version: String = "test-version"
  val oas: String = "test-oas"

  val deployments: Seq[ApiDeployment] = Seq(
    ApiDeployment("test-id-1", Some(Instant.now())),
    ApiDeployment("test-id-2", None)
  )

  val deployment: SuccessfulDeploymentResponse = SuccessfulDeploymentResponse(
    id = "test-deployment-id",
    deploymentTimestamp = Some(Instant.now()),
    deploymentVersion = Some("test-deployment-version"),
    oasVersion = "test-oas-version",
    buildVersion = Some("test-build-version")
  )

  val deploymentDetails: DeploymentDetails = DeploymentDetails(
    description = Some("test-description"),
    status = Some("test-status"),
    domain = Some("test-domain"),
    subDomain = Some("test-sub-domain"),
    hods = Some(Seq("test-hod")),
    egressMappings = Some(Seq(EgressMapping("test-prefix", "test-egress-prefix"))),
    prefixesToRemove = Some(Seq("test-prefix-to-remove")),
    egress = Some("test-egress")
  )

  val statusResponse: StatusResponse = StatusResponse(
    status = "test-status",
    message = Some("test-message"),
    health = Some("test-health")
  )

  val egressGateways: Seq[EgressGateway] = Seq(
    EgressGateway(
      id = "test-id-1",
      friendlyName = "test-friendly-name-1"
    ),
    EgressGateway(
      id = "test-id-2",
      friendlyName = "test-friendly-name-2"
    )
  )

  val clientScopes: Seq[ClientScope] = Seq(
    ClientScope("test-scope-1"),
    ClientScope("test-scope-2")
  )

  def prettyPrint[T](t: T)(implicit writesT: Writes[T]): String = {
    Json.prettyPrint(Json.toJson(t))
  }

}
