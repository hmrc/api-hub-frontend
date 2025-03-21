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

import config.HipEnvironments
import viewmodels.govuk.all.SelectItemViewModel
import viewmodels.govuk.all.FluentSelectItem

case class TestApimEndpointsViewModel (hipEnvironments: HipEnvironments)(implicit messages: play.api.i18n.Messages) {
  val environments = Seq(SelectItemViewModel("", messages("testApimEndpoints.noEnvironmentSelection"))) ++ hipEnvironments.environments.reverse.map(env => SelectItemViewModel(env.id, env.nameKey))
  val endpoints = Seq(SelectItemViewModel("", messages("testApimEndpoints.noEndpointSelection"))) ++ ApimRequests.requests.map(apimRequest => SelectItemViewModel(apimRequest.id, apimRequest.url).withAttribute("data-param-names", apimRequest.paramNames.mkString(",")))
}
