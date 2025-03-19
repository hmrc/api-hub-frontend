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

import controllers.IndexControllerSpec.mock
import fakes.FakeHipEnvironments
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import viewmodels.govuk.all.SelectItemViewModel
import viewmodels.govuk.all.FluentSelectItem

class TestApimEndpointsViewModelSpec extends AnyFreeSpec with Matchers {

  "TestApimEndpointsViewModel" - {
    "must return correct environment details" in {
      val messages = mock[Messages]
      when(messages.apply(anyString(), any())).thenAnswer(p => p.getArgument(0))
      val viewModel = TestApimEndpointsViewModel(FakeHipEnvironments)(messages)

      viewModel.environments mustBe Seq(
        SelectItemViewModel("", "testApimEndpoints.noEnvironmentSelection"),
        SelectItemViewModel("test", "site.environment.test"),
        SelectItemViewModel("preprod", "site.environment.preprod"),
        SelectItemViewModel("production", "site.environment.production"),
      )
    }

    "must return correct request details" in {
      val messages = mock[Messages]
      when(messages.apply(anyString(), any())).thenAnswer(p => p.getArgument(0))
      val viewModel = TestApimEndpointsViewModel(FakeHipEnvironments)(messages)

      viewModel.endpoints mustBe Seq(
        SelectItemViewModel("", "testApimEndpoints.noEndpointSelection"),
        SelectItemViewModel("getDeployments", "/v1/oas-deployments").withAttribute("data-param-names", ""),
        SelectItemViewModel("getDeployment", "/v1/oas-deployments/{id}").withAttribute("data-param-names", "id"),
        SelectItemViewModel("getOpenApiSpecification", "/v1/oas-deployments/{id}/oas").withAttribute("data-param-names", "id"),
        SelectItemViewModel("getDeploymentDetails", "/v1/simple-api-deployment/deployments/{serviceId}").withAttribute("data-param-names", "serviceId"),
        SelectItemViewModel("getDeploymentStatus", "/v1/simple-api-deployment/deployments/{serviceId}/status?mr-iid={mergeRequestIid}&version={version}").withAttribute("data-param-names", "serviceId,mergeRequestIid,version"),
        SelectItemViewModel("listEgressGateways", "/v1/simple-api-deployment/egress-gateways").withAttribute("data-param-names", ""),
        SelectItemViewModel("fetchClientScopes", "/identity/clients/{id}/client-scopes").withAttribute("data-param-names", "id"),
      )
    }
  }

}
