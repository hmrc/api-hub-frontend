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
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.StatisticsView

class StatisticsControllerSpec
  extends SpecBase
    with MockitoSugar
    with TestHelpers
    with HtmlValidation {

  "StatisticsController" - {
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

  private case class Fixture(application: Application)

  private def buildFixture(user: UserModel): Fixture = {
    val application = applicationBuilder(user = user)
      .build()

    Fixture(application)
  }

}
