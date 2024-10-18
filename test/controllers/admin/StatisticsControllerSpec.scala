/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.admin

import base.SpecBase
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.{Application, Application as PlayApplication}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.ApiHubService
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.StatisticsView
import org.mockito.Mockito.when
import scala.concurrent.Future
import models.stats.ApisInProductionStatistic
import models.api.ApiDetailLensesSpec.sampleApiDetailSummary

class StatisticsControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  "StatisticsController" - {
    "onPageLoad" - {
      "must return Ok and the correct view for a support user" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
  
          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.onPageLoad())
            val result = route(fixture.application, request).value
            val view = fixture.application.injector.instanceOf[StatisticsView]
  
            status(result) mustBe OK
            contentAsString(result) mustBe view(user)(request, messages(fixture.application)).toString
            contentAsString(result) must validateAsHtml
          }
        }
      }
  
      "must return Unauthorized for a non-support user" in {
        forAll(usersWhoCannotSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.onPageLoad())
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }
    }
    
    "apisInProduction" - {
      "must return Ok and the JSON for a support user" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
          val stats = ApisInProductionStatistic(10, 5)
          when(fixture.apiHubService.apisInProduction()(any)).thenReturn(Future.successful(stats))

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.apisInProduction())
            val result = route(fixture.application, request).value

            status(result) mustBe OK
            contentAsJson(result) mustBe Json.toJson(stats)
          }
        }
      }

      "must return Unauthorized for a non-support user" in {
        forAll(usersWhoCannotSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.apisInProduction())
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }      
    }

    "listApisInProduction" - {
      "must return Ok and the sorted list of API names for a support user" in {
        forAll(usersWhoCanSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)
          val apis = Seq(
            sampleApiDetailSummary().copy(title = "Api 2"),
            sampleApiDetailSummary().copy(title = "api 1"),
            sampleApiDetailSummary().copy(title = "api 3")
          )
          when(fixture.apiHubService.listApisInProduction()(any)).thenReturn(Future.successful(apis))

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.listApisInProduction())
            val result = route(fixture.application, request).value

            status(result) mustBe OK
            contentAsJson(result) mustBe Json.toJson(Seq("api 1", "Api 2", "api 3"))
          }
        }
      }

      "must return Unauthorized for a non-support user" in {
        forAll(usersWhoCannotSupport) { (user: UserModel) =>
          val fixture = buildFixture(user)

          running(fixture.application) {
            val request = FakeRequest(controllers.admin.routes.StatisticsController.listApisInProduction())
            val result = route(fixture.application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
          }
        }
      }
    }
  }

  private case class Fixture(application: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
