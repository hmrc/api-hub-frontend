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
import controllers.routes
import generators.ApiDetailGenerators
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq => eqTo, isNull}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.Application as PlayApplication
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.ManageApisView

import scala.concurrent.Future

class ManageApisControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with ApiDetailGenerators
    with HtmlValidation {

  "ManageApisController" - {
    "must return the list of apis to a user with the support role" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)
        val apis = Seq(sampleApiDetailWithoutOAS())

        when(fixture.apiHubService.getApis(any)(any)).thenReturn(Future.successful(apis))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.admin.routes.ManageApisController.onPageLoad())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe OK

          verify(fixture.apiHubService).getApis(isNull)(any)

          val view = fixture.playApplication.injector.instanceOf[ManageApisView]
          contentAsString(result) mustBe view(apis, user)(request, messages(fixture.playApplication)).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }

    "must redirect to the unauthorised page for a user who is not support" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.admin.routes.ManageApisController.onPageLoad())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }

}
