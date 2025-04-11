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
import controllers.actions.{FakeSupporter, FakeUser}
import controllers.team.CreateTeamCheckYourAnswersControllerSpec.{buildTeamDetailsSummaryList, buildTeamMembersSummaryList}
import forms.YesNoFormProvider
import generators.Generators
import models.application.TeamMember
import models.exception.TeamNameNotUniqueException
import models.{CheckMode, UserAnswers}
import models.team.{NewTeam, Team, TeamType}
import models.user.UserModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.{CreateTeamApiProducerConsumerPage, CreateTeamMembersPage, CreateTeamNamePage}
import play.api.Application as PlayApplication
import play.api.http.Status.OK
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.CreateTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.HtmlValidation
import viewmodels.checkAnswers.{CreateTeamAddTeamMemberSummary, CreateTeamApiProducerConsumerSummary, CreateTeamNameSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.team.{CreateTeamCheckYourAnswersView, CreateTeamSuccessView}

import java.time.LocalDateTime
import scala.concurrent.Future

class CreateTeamCheckYourAnswersControllerSpec extends SpecBase
  with SummaryListFluency
  with ScalaCheckPropertyChecks
  with Generators
  with HtmlValidation
  with MockitoSugar {

  import CreateTeamCheckYourAnswersControllerSpec.*

  private lazy val form = YesNoFormProvider()("createTeamCheckYourAnswers.producingAPIs.confirmation.error")

  "Check Your Answers Controller" - {
    "onPageLoad" - {
      "must return OK and the correct view for a GET with empty user answers" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[CreateTeamCheckYourAnswersView]
          val teamDetails = buildTeamDetailsSummaryList(emptyUserAnswers, messages(application))
          val teamMemberDetails = buildTeamMembersSummaryList(emptyUserAnswers)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(teamDetails, teamMemberDetails, Some(FakeUser), false, form)(request, messages(application)).toString
          contentAsString(result) must validateAsHtml
        }
      }

      "must return OK and the correct view for a GET with complete user answers" in {

        forAll((userAnswers: UserAnswers) => {
          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, controllers.team.routes.CreateTeamCheckYourAnswersController.onPageLoad().url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CreateTeamCheckYourAnswersView]
            val teamDetails = buildTeamDetailsSummaryList(userAnswers, messages(application))
            val teamMemberDetails = buildTeamMembersSummaryList(userAnswers)

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(teamDetails, teamMemberDetails, Some(FakeUser), false, form)(request, messages(application)).toString
            contentAsString(result) must validateAsHtml
          }
        })

      }
    }

    "onSubmit" - {
      "must create the team, clear user answers, and redirect to the team created page" in {
        val fixture = buildFixture(userAnswers = fullAnswers)

        when(fixture.apiHubService.createTeam(any)(any)).thenReturn(Future.successful(Right(team)))
        when(fixture.sessionRepository.clear(any)).thenReturn(Future.successful(true))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[CreateTeamSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(team, Some(FakeSupporter))(request, messages(fixture.playApplication)).toString
          contentAsString(result) must validateAsHtml
        }
      }

      "must create the team, clear user answers, and redirect to the team created page when the team is a producer team and the ack checkbox is ticked" in {
        val fixture = buildFixture(userAnswers = fullAnswers.set(CreateTeamApiProducerConsumerPage, true).get)

        when(fixture.apiHubService.createTeam(any)(any)).thenReturn(Future.successful(Right(team.copy(teamType = TeamType.ProducerTeam))))
        when(fixture.sessionRepository.clear(any)).thenReturn(Future.successful(true))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
            .withFormUrlEncodedBody("value" -> true.toString)
          val result = route(fixture.playApplication, request).value
          val view = fixture.playApplication.injector.instanceOf[CreateTeamSuccessView]

          status(result) mustBe OK
          contentAsString(result) mustBe view(team, Some(FakeSupporter))(request, messages(fixture.playApplication)).toString
          contentAsString(result) must validateAsHtml
        }
      }

      "must return a bad request when the team is a producer team and the ack checkbox is not ticked" in {
        val userAnswers = fullAnswers
          .set(CreateTeamApiProducerConsumerPage, true).get
        val fixture = buildFixture(userAnswers = userAnswers)

        when(fixture.apiHubService.createTeam(any)(any)).thenReturn(Future.successful(Right(team.copy(teamType = TeamType.ProducerTeam))))
        when(fixture.sessionRepository.clear(any)).thenReturn(Future.successful(true))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val formWithErrors = form.withError("value", "createTeamCheckYourAnswers.producingAPIs.confirmation.error")
          val result = route(fixture.playApplication, request).value

          val view = fixture.playApplication.injector.instanceOf[CreateTeamCheckYourAnswersView]
          val teamDetails = buildTeamDetailsSummaryList(userAnswers, messages(fixture.playApplication))
          val teamMemberDetails = buildTeamMembersSummaryList(userAnswers)


          status(result) mustBe BAD_REQUEST
          contentAsString(result) mustEqual view(teamDetails, teamMemberDetails, Some(FakeSupporter), true, formWithErrors)(request, messages(fixture.playApplication)).toString
          contentAsString(result) must validateAsHtml
        }
      }

      "must redirect to the team name page if there is no team name answer" in {
        val answers = UserAnswers(FakeUser.userId).set(CreateTeamMembersPage, teamMembers).toOption.value

        val fixture = buildFixture(
          userAnswers = answers,
          user = FakeSupporter
        )

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode).url
        }
      }

      "must redirect to the team name page if the team name is not unique" in {
        val fixture = buildFixture(userAnswers = fullAnswers)

        when(fixture.apiHubService.createTeam(any)(any))
          .thenReturn(Future.successful(Left(TeamNameNotUniqueException.forName(teamName))))

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.team.routes.CreateTeamNameController.onPageLoad(CheckMode).url

          verifyNoInteractions(fixture.sessionRepository)
        }
      }

      "must redirect to the journey recovery page if there is no team members answer" in {
        val answers = fullAnswers.remove(CreateTeamMembersPage).get

        val fixture = buildFixture(userAnswers = answers)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the team type page if there is no team type answer" in {
        val answers = fullAnswers.remove(CreateTeamApiProducerConsumerPage).get

        val fixture = buildFixture(userAnswers = answers)

        running(fixture.playApplication) {
          val request = FakeRequest(controllers.team.routes.CreateTeamCheckYourAnswersController.onSubmit())
          val result = route(fixture.playApplication, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.team.routes.ManageTeamProducerConsumerController.onPageLoad(CheckMode).url
        }
      }
    }
  }

  private case class Fixture(
                              playApplication: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: CreateTeamSessionRepository
                            )

  private def buildFixture(
                            team: Option[Team] = None,
                            userAnswers: UserAnswers = UserAnswers("id"),
                            user: UserModel = FakeSupporter
                          ): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[CreateTeamSessionRepository]
    when(apiHubService.findTeamById(any)(any)).thenReturn(Future.successful(team))

    val playApplication = applicationBuilder(Some(userAnswers), user)
      .overrides(
        bind[ApiHubService].to(apiHubService),
        bind[CreateTeamSessionRepository].to(sessionRepository)
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }
}

object CreateTeamCheckYourAnswersControllerSpec extends SummaryListFluency {

  private val teamName = "test-team-name"
  private val teamMembers = Seq(TeamMember(FakeUser.email))
  private val team = Team("test-team-id", teamName, LocalDateTime.now(), teamMembers)

  private val fullAnswers = UserAnswers(FakeUser.userId)
    .set(CreateTeamNamePage, teamName).get
    .set(CreateTeamMembersPage, teamMembers).get
    .set(CreateTeamApiProducerConsumerPage, false).get

  def buildTeamDetailsSummaryList(userAnswers: UserAnswers, messages: Messages): SummaryList = {
    SummaryListViewModel(
      Seq(
        CreateTeamNameSummary.row(userAnswers)(messages),
        CreateTeamApiProducerConsumerSummary.row(userAnswers)(messages)
      ).flatten
    )
  }

  def buildTeamMembersSummaryList(userAnswers: UserAnswers): SummaryList = {
    CreateTeamAddTeamMemberSummary.summary(userAnswers)
  }

}
