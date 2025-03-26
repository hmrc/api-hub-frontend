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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import config.{FrontendAppConfig, HipEnvironment}
import fakes.FakeHipEnvironments
import models.api.{ApiDeployment, EgressGateway}
import models.application.ClientScope
import models.deployment.{DeploymentDetails, EgressMapping, StatusResponse, SuccessfulDeploymentResponse}
import models.exception.ApimException
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Configuration
import play.api.http.HeaderNames.*
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Instant

class ApimConnectorSpec extends AsyncFreeSpec with Matchers with WireMockSupport with HttpClientV2Support {

  import ApimConnectorSpec.*

  "getDeployments" - {
    "must return the deployments when available" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployments/${environment.id}"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(deployments).toString)
          )
      )

      buildConnector().getDeployments(environment).map {
        result =>
          result mustBe Right(deployments)
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployments/${environment.id}"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().getDeployments(environment).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployments/${environment.id}"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().getDeployments(environment).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "getDeployment" - {
    "must return the deployment when its exists" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment/${environment.id}/$publisherRef"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(deployment).toString)
          )
      )

      buildConnector().getDeployment(environment, publisherRef).map {
        result =>
          result mustBe Right(deployment)
      }
    }

    "must return NotFound when the deployment does not exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getDeployment(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.serviceNotFound(publisherRef))
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().getDeployment(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().getDeployment(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "getOpenApiSpecification" - {
    "must return the OAS document when it exists" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-open-api-specification/${environment.id}/$publisherRef"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/yaml"))
          .willReturn(
            aResponse()
              .withBody(oas)
          )
      )

      buildConnector().getOpenApiSpecification(environment, publisherRef).map {
        result =>
          result mustBe Right(oas)
      }
    }

    "must return NotFound when the deployment does not exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-open-api-specification/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getOpenApiSpecification(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.serviceNotFound(publisherRef))
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-open-api-specification/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().getOpenApiSpecification(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-open-api-specification/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().getOpenApiSpecification(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "getDeploymentDetails" - {
    "must return the deployment details when they exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-details/${environment.id}/$publisherRef"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(deploymentDetails).toString)
          )
      )

      buildConnector().getDeploymentDetails(environment, publisherRef).map {
        result =>
          result mustBe Right(deploymentDetails)
      }
    }

    "must return NotFound when the deployment does not exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-details/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getDeploymentDetails(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.serviceNotFound(publisherRef))
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-details/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().getDeploymentDetails(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-details/${environment.id}/$publisherRef"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().getDeploymentDetails(environment, publisherRef).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "getDeploymentStatus" - {
    "must return the deployment status when it exists" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-status/${environment.id}/$publisherRef?mergeRequestIid=$mergeRequestIid&version=$version"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(statusResponse).toString)
          )
      )

      buildConnector().getDeploymentStatus(environment, publisherRef, mergeRequestIid, version).map {
        result =>
          result mustBe Right(statusResponse)
      }
    }

    "must return NotFound when the deployment does not exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-status/${environment.id}/$publisherRef?mergeRequestIid=$mergeRequestIid&version=$version"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().getDeploymentStatus(environment, publisherRef, mergeRequestIid, version).map {
        result =>
          result mustBe Left(ApimException.serviceNotFound(publisherRef))
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-status/${environment.id}/$publisherRef?mergeRequestIid=$mergeRequestIid&version=$version"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().getDeploymentStatus(environment, publisherRef, mergeRequestIid, version).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/get-deployment-status/${environment.id}/$publisherRef?mergeRequestIid=$mergeRequestIid&version=$version"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().getDeploymentStatus(environment, publisherRef, mergeRequestIid, version).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "listEgressGateways" - {
    "must return the egress gateways for an environment" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/list-egress-gateways/${environment.id}"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(egressGateways).toString)
          )
      )

      buildConnector().listEgressGateways(environment).map {
        result =>
          result mustBe Right(egressGateways)
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/list-egress-gateways/${environment.id}"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().listEgressGateways(environment).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/list-egress-gateways/${environment.id}"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().listEgressGateways(environment).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  "fetchClientScopes" - {
    "must fetch the client scopes when they exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/fetch-client-scopes/${environment.id}/$clientId"))
          .withHeader(AUTHORIZATION, equalTo("An authentication token"))
          .withHeader(ACCEPT, equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody(Json.toJson(clientScopes).toString)
          )
      )

      buildConnector().fetchClientScopes(environment, clientId).map {
        result =>
          result mustBe Right(clientScopes)
      }
    }

    "must return NotFound when the credential does not exist" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/fetch-client-scopes/${environment.id}/$clientId"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      buildConnector().fetchClientScopes(environment, clientId).map {
        result =>
          result mustBe Left(ApimException.credentialNotFound(clientId))
      }
    }

    "must return BadGateway when the response is 502 Bad Gateway" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/fetch-client-scopes/${environment.id}/$clientId"))
          .willReturn(
            aResponse()
              .withStatus(BAD_GATEWAY)
          )
      )

      buildConnector().fetchClientScopes(environment, clientId).map {
        result =>
          result mustBe Left(ApimException.badGateway())
      }
    }

    "must return UnexpectedResponse for other responses" in {
      stubFor(
        get(urlEqualTo(s"/api-hub-applications/support/apim/fetch-client-scopes/${environment.id}/$clientId"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )

      buildConnector().fetchClientScopes(environment, clientId).map {
        result =>
          result mustBe Left(ApimException.unexpectedResponse(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def buildConnector(): ApimConnector = {
    val servicesConfig = new ServicesConfig(
      Configuration.from(Map(
        "microservice.services.api-hub-applications.host" -> wireMockHost,
        "microservice.services.api-hub-applications.port" -> wireMockPort
      ))
    )

    val application = new GuiceApplicationBuilder().build()
    val frontendAppConfig =  application.injector.instanceOf[FrontendAppConfig]

    new ApimConnector(httpClientV2, servicesConfig, frontendAppConfig)
  }

}

private object ApimConnectorSpec {

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

}
