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
import generators.Generators
import models.UserAnswers
import models.api.EgressGateway
import models.team.Team
import models.user.UserModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.HtmlValidation
import viewmodels.admin.AssignTeamEgressesViewModel
import viewmodels.govuk.SummaryListFluency
import views.html.admin.addegresstoteam.TeamEgressCheckYourAnswersView
import pages.admin.addegresstoteam.{AddEgressToTeamTeamPage, SelectTeamEgressesPage}
import play.api.inject.bind
import services.ApiHubService
import play.api.Application as PlayApplication

import java.time.LocalDateTime
import scala.concurrent.Future
class TeamEgressCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with ScalaCheckPropertyChecks with Generators with HtmlValidation {

  "Check Your Answers Controller" - {

    val gateway = EgressGateway("egress1", "an egress")
    val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty)

    "must return OK and the correct view for a GET for supporter with empty user answers" in {

      val fixture = buildFixture(FakeSupporter, userAnswers = Some(emptyUserAnswers))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[TeamEgressCheckYourAnswersView]
        val viewModel = AssignTeamEgressesViewModel(sampleTeam(), Seq(gateway))
        status(result) mustEqual SEE_OTHER
      }
    }

    "must display page populated correctly for a GET for support users with user answers" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, team).success.value
        .set(SelectTeamEgressesPage, Set("egress1")).success.value

      val fixture = buildFixture(FakeSupporter, userAnswers = Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(Seq(gateway)))
      running(fixture.playApplication) {
        val view = fixture.playApplication.injector.instanceOf[TeamEgressCheckYourAnswersView]
        val request = FakeRequest(GET, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.playApplication, request).value
        val viewModel = AssignTeamEgressesViewModel(team, Seq(gateway))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel, FakeSupporter)(request, messages(fixture.playApplication)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to unauthorised page for GET if not a support user" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, team).success.value
        .set(SelectTeamEgressesPage, Set("egress1")).success.value

      val fixture = buildFixture(FakeUser, userAnswers =  Some(userAnswers))

      running(fixture.playApplication) {
        val request = FakeRequest(GET, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "must return OK and the correct view for a POST for supporter with empty user answers" in {

      val fixture = buildFixture(FakeSupporter, userAnswers = Some(emptyUserAnswers))

      running(fixture.playApplication) {
        val request = FakeRequest(POST, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)

        val result = route(fixture.playApplication, request).value

        val view = fixture.playApplication.injector.instanceOf[TeamEgressCheckYourAnswersView]
        val viewModel = AssignTeamEgressesViewModel(sampleTeam(), Seq(gateway))
        status(result) mustEqual SEE_OTHER
      }
    }

    "must display page populated correctly for a POST for support users with user answers" in {

      val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty)
      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, team).success.value
        .set(SelectTeamEgressesPage, Set("egress1")).success.value

      val fixture = buildFixture(FakeSupporter, userAnswers = Some(userAnswers))

      when(fixture.apiHubService.addEgressesToTeam(any(), any())(any())).thenReturn(Future.successful(Some(())))
      running(fixture.playApplication) {
        val view = fixture.playApplication.injector.instanceOf[TeamEgressCheckYourAnswersView]
        val request = FakeRequest(POST, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.playApplication, request).value
        val viewModel = AssignTeamEgressesViewModel(team, Seq(gateway))

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.admin.addegresstoteam.routes.TeamEgressSuccessController.onPageLoad().url)
      }
    }

    "must redirect to unauthorised page for POST if not a support user" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, team).success.value
        .set(SelectTeamEgressesPage, Set("egress1")).success.value

      val fixture = buildFixture(FakeUser, userAnswers = Some(userAnswers))

      running(fixture.playApplication) {
        val request = FakeRequest(POST, controllers.admin.addegresstoteam.routes.TeamEgressCheckYourAnswersController.onPageLoad().url)
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.UnauthorisedController.onPageLoad.url)
      }
    }
  }

  private case class Fixture(playApplication: PlayApplication, apiHubService: ApiHubService)

  private def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers] = None): Fixture = {
    val apiHubService = mock[ApiHubService]

    val playApplication = applicationBuilder(userAnswers = userAnswers, user = userModel)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService)
      ).build()

    Fixture(playApplication, apiHubService)
  }
}

