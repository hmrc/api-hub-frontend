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

package controllers

import base.OptionallyAuthenticatedSpecBase
import generators.ApiDetailGenerators
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApiHubService
import utils.HtmlValidation

import scala.concurrent.Future

class DeepSearchApisControllerSpec
  extends OptionallyAuthenticatedSpecBase
    with MockitoSugar
    with ScalaCheckDrivenPropertyChecks
    with ApiDetailGenerators
    with HtmlValidation
    with OptionValues {

  "GET" - {
    "must return OK and the correct view for an unauthenticated user" in {
      val fixture = buildFixture()
      val searchTerm = "nps"
      val results = (1 to 10).map(_ => sampleApiDetail())

      running(fixture.application) {
          when(fixture.apiHubService.deepSearchApis(any())(any()))
            .thenReturn(Future.successful(results))

          val request = FakeRequest(GET, routes.DeepSearchApisController.doSearch(searchTerm).url)
          val result = route(fixture.application, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(results.map(_.id))
        }
    }
 }

  private case class Fixture(apiHubService: ApiHubService, application: Application)

  private def buildFixture(userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]

    val application = applicationBuilder(userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      )
      .build()

    Fixture(apiHubService, application)
  }
}
