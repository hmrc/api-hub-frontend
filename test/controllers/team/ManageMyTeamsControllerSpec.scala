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

package controllers.team

import base.SpecBase
import controllers.actions.FakeUser
import models.user.UserModel
import org.scalatest.OptionValues
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import utils.HtmlValidation
import views.html.team.ManageMyTeamsView

class ManageMyTeamsControllerSpec extends SpecBase with HtmlValidation with OptionValues {

  "ManageMyTeamsController" - {
    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture()

      running(fixture.playApplication) {
        val request = FakeRequest(routes.ManageMyTeamsController.onPageLoad())
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[ManageMyTeamsView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication)

  private def buildFixture(userModel: UserModel = FakeUser): Fixture = {
    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .build()

    Fixture(playApplication)
  }

}
