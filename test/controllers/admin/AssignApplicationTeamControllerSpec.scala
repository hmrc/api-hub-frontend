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
import generators.TeamGenerator
import models.user.UserModel
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import utils.{HtmlValidation, TestHelpers}
import views.html.admin.AssignApplicationTeamView

import java.util.UUID

class AssignApplicationTeamControllerSpec
  extends SpecBase
    with MockitoSugar
    with ArgumentMatchersSugar
    with TestHelpers
    with HtmlValidation
    with TeamGenerator {

  "AssignApplicationTeamController" - {
    "must return Ok and the correct view for an approver or support user" in {
      forAll(usersWhoCanSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val applicationId = UUID.randomUUID().toString()
          implicit val msgs: Messages = messages(fixture.playApplication)
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
            GET,
            controllers.admin.routes.AssignApplicationTeamController.onPageLoad(applicationId).url
          )
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[AssignApplicationTeamView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(user).toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }
    "must return Unauthorized for a non-support user" in {
      forAll(usersWhoCannotSupport) { (user: UserModel) =>
        val fixture = buildFixture(user)

        running(fixture.playApplication) {
          val applicationId = UUID.randomUUID().toString()
          implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
            GET,
            controllers.admin.routes.AssignApplicationTeamController.onPageLoad(applicationId).url
          )
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication)

  private def buildFixture(userModel: UserModel): Fixture = {
    val playApplication = applicationBuilder(userAnswers = Some(emptyUserAnswers), user = userModel)
      .build()

    Fixture(playApplication)
  }
}