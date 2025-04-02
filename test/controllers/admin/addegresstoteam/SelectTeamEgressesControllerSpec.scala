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
import forms.admin.addegresstoteam.SelectTeamEgressFormProvider
import models.api.EgressGateway
import models.team.Team
import models.user.UserModel
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.admin.addegresstoteam.{AddEgressToTeamTeamPage, SelectTeamEgressesPage}
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.AddEgressToTeamSessionRepository
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier
import utils.HtmlValidation
import views.html.ErrorTemplate
import views.html.admin.addegresstoteam.SelectTeamEgressView

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import scala.concurrent.Future

class SelectTeamEgressesControllerSpec extends SpecBase with MockitoSugar with HtmlValidation {
  import SelectTeamEgressesControllerSpec.*

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val allTestEgresses = Seq(
    EgressGateway("e1", "egress1"),
    EgressGateway("e2", "egress2"),
    EgressGateway("e3", "egress3")
  )
  val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty)

  "AddEgressToTeamStartController" - {
    "must display page populated correctly for support users" in {
      val userAnswers = UserAnswers(userAnswersId).set(AddEgressToTeamTeamPage, team).success.value
      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, team, allTestEgresses, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must prepopulate page if values have already been entered" in {
      val selectedEgresses = Set("e1", "e2")
      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, team).success.value
        .set(SelectTeamEgressesPage, selectedEgresses).success.value

      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(selectedEgresses), team, allTestEgresses, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must not include egresses in the list if the team already has access to them" in {
      val teamWithAnEgress = team.copy(egresses = Seq("e1"))
      val userAnswers = UserAnswers(userAnswersId)
        .set(AddEgressToTeamTeamPage, teamWithAnEgress).success.value

      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, team, allTestEgresses.filterNot(_.id == "e1"), FakeSupporter)(request, messages(fixture.application)).toString

        contentAsString(result) must validateAsHtml
      }
    }

    "egresses must be displayed in case-insensitive alphabetical order" in {
      val userAnswers = UserAnswers(userAnswersId).set(AddEgressToTeamTeamPage, team).success.value
      val fixture = buildFixture(FakeSupporter, Some(userAnswers))
      val egressesWithMixedCaseOutOfOrder = Seq(
        EgressGateway("e1", "egress C"),
        EgressGateway("e2", "Egress A"),
        EgressGateway("e3", "egreSS b")
      )
      val egressesSortedAlphabetically = Seq(
        EgressGateway("e2", "Egress A"),
        EgressGateway("e3", "egreSS b"),
        EgressGateway("e1", "egress C")
      )

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(egressesWithMixedCaseOutOfOrder))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, team, egressesSortedAlphabetically, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "selected egresses must be saved to the database" in {
      val userAnswers = UserAnswers(userAnswersId).set(AddEgressToTeamTeamPage, team).success.value
      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(POST, controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onSubmit(NormalMode).url).withFormUrlEncodedBody(("value[0]", "e1"), ("value[1]", "e2"))
        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER

        val updatedUserAnswers = userAnswers.set(SelectTeamEgressesPage, Set("e1", "e2")).success.value
        verify(fixture.sessionRepository).set(eqTo(updatedUserAnswers))
      }
    }

    "page must be redisplayed with an error message if no egress is selected" in {
      val userAnswers = UserAnswers(userAnswersId).set(AddEgressToTeamTeamPage, team).success.value
      val fixture = buildFixture(FakeSupporter, Some(userAnswers))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[SelectTeamEgressView]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(POST, controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onSubmit(NormalMode).url)
        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(form.bind(Map()), team, allTestEgresses, FakeSupporter)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to unauthorised page for GET request if not a support user" in {
      val fixture = buildFixture(FakeUser)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

        verify(fixture.apiHubService, never()).listEgressGateways(any())(any())
      }
    }

    "must redirect to unauthorised page for POST request if not a support user" in {
      val fixture = buildFixture(FakeUser)

      running(fixture.application) {
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onSubmit(NormalMode))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)

        verify(fixture.apiHubService, never()).listEgressGateways(any())(any())
        verify(fixture.sessionRepository, never()).set(any())
      }

    }

    "must display team not found page if team id is invalid" in {
      val userAnswersWithoutTeam = UserAnswers(userAnswersId)
      val fixture = buildFixture(FakeSupporter, Some(userAnswersWithoutTeam))

      when(fixture.apiHubService.listEgressGateways(any())(any())).thenReturn(Future.successful(allTestEgresses))

      running(fixture.application) {
        val view = fixture.application.injector.instanceOf[ErrorTemplate]
        val form = SelectTeamEgressFormProvider()()
        val request = FakeRequest(controllers.admin.addegresstoteam.routes.SelectTeamEgressesController.onPageLoad(NormalMode))
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

    case class Fixture(application: Application, apiHubService: ApiHubService, sessionRepository: AddEgressToTeamSessionRepository)

    def buildFixture(userModel: UserModel, userAnswers: Option[UserAnswers] = None): Fixture = {
      val apiHubService = mock[ApiHubService]
      val sessionRepository = mock[AddEgressToTeamSessionRepository]
      val application = applicationBuilder(userAnswers, userModel)
        .overrides(
          bind[ApiHubService].toInstance(apiHubService),
          bind[AddEgressToTeamSessionRepository].toInstance(sessionRepository),
        )
        .build()

      Fixture(application, apiHubService, sessionRepository)
    }
  }
}

object SelectTeamEgressesControllerSpec {

  private val nextPage = controllers.routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

}
