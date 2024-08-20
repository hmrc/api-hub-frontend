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

package controllers.application.register

import base.SpecBase
import controllers.actions.FakeUser
import generators.TeamGenerator
import models.UserAnswers
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationTeamPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.HtmlValidation
import viewmodels.checkAnswers.application.register.{RegisterApplicationNameSummary, RegisterApplicationTeamSummary}
import views.html.application.register.RegisterApplicationCheckYourAnswersView

class RegisterApplicationCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar with HtmlValidation with TeamGenerator {

  "RegisterApplicationCheckYourAnswersController" - {

    "must return OK and the correct view" in {
      val team = sampleTeam()
      val userAnswers = UserAnswers(userAnswersId)
        .set(RegisterApplicationNamePage, "name").success.value
        .set(RegisterApplicationTeamPage, team).success.value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.application.register.routes.RegisterApplicationCheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        val view = application.injector.instanceOf[RegisterApplicationCheckYourAnswersView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          RegisterApplicationNameSummary.row(userAnswers)(messages(application)),
          RegisterApplicationTeamSummary.row(userAnswers)(messages(application)),
          Some(FakeUser))(request, messages(application)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }

}
