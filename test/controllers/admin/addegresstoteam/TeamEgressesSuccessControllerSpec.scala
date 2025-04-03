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

package controllers.admin.addegresstoteam

import base.SpecBase
import controllers.actions.{FakeSupporter, FakeUser}
import controllers.routes
import models.UserAnswers
import models.team.Team
import models.user.UserModel
import org.scalatestplus.mockito.MockitoSugar
import pages.admin.addegresstoteam.AddEgressToTeamTeamPage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.admin.addegresstoteam.TeamEgressSuccessView

import java.time.LocalDateTime

class TeamEgressesSuccessControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty)

  "TeamEgressesSuccessControllerSpec" - {
    "must display page populated correctly for support users" in {
      val userAnswers = UserAnswers(userAnswersId).set(AddEgressToTeamTeamPage, team).success.value
      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[TeamEgressSuccessView]
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.TeamEgressSuccessController.onPageLoad())
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(team, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must display team not found page if team id is invalid" in {
      val userAnswersWithoutTeam = UserAnswers(userAnswersId)
      val fixture = buildFixture(FakeSupporter, Some(userAnswersWithoutTeam))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.TeamEgressSuccessController.onPageLoad())
        val result = route(fixture.application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "Team not found",
            s"Cannot find a team with ID unknown.",
            Some(FakeSupporter)
          )(request, messages(fixture.application))
            .toString()
        contentAsString(result) must validateAsHtml
      }
    }
    
    "must redirect to unauthorised page if not a support user" in {
      val fixture = buildFixture(FakeUser)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.TeamEgressSuccessController.onPageLoad())
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    case class Fixture(application: Application)

    def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers] = None): Fixture = {
      val application = applicationBuilder(userAnswers, userModel).build()
      Fixture(application)
    }
  }
}
