/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.myapis.produce


import base.SpecBase
import controllers.actions.FakeUser
import fakes.FakeHipEnvironments
import forms.myapis.produce.ProduceApiSelectEgressForm
import generators.EgressGenerator
import models.api.EgressGateway
import models.team.Team
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.{ProduceApiChooseTeamPage, ProduceApiSelectEgressPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.myapis.produce.{ProduceApiSelectEgressFormViewModel, ProduceApiSelectEgressViewModel}
import views.html.myapis.produce.ProduceApiSelectEgressView

import java.time.LocalDateTime
import scala.concurrent.Future

class ProduceApiSelectEgressControllerSpec extends SpecBase with MockitoSugar with EgressGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = ProduceApiSelectEgressForm()
  private val form = formProvider()
  private val allEgresses = Seq(
    EgressGateway("eg1", "Egress 1"),
    EgressGateway("eg2", "Egress 2"),
    EgressGateway("eg3", "Egress 3"),
    EgressGateway("eg4", "Egress 4"),
    EgressGateway("eg5", "Egress 5"),
  )
  private val team = Team("teamId", "teamName", LocalDateTime.now(), Seq.empty, egresses = Seq("eg1", "eg4"))
  private val userAnswersWithTeam = emptyUserAnswers.set(ProduceApiChooseTeamPage, team).success.value
  private val teamEgresses = Seq(
    EgressGateway("eg1", "Egress 1"),
    EgressGateway("eg4", "Egress 4"),
  )

  private lazy val produceApiSelectEgressRoute = controllers.myapis.produce.routes.ProduceApiSelectEgressController.onPageLoad(NormalMode).url

  "ProduceApiSelectEgressController" - {

    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(userAnswers = Some(userAnswersWithTeam))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(allEgresses))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiSelectEgressRoute)
        val result = route(fixture.application, request).value
        val viewModel = ProduceApiSelectEgressViewModel(
          "myApis.produce.selectegress.title",
          teamEgresses,
          controllers.myapis.produce.routes.ProduceApiSelectEgressController.onSubmit(NormalMode)
        )

        val view = fixture.application.injector.instanceOf[ProduceApiSelectEgressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the question has already been answered" in {
      val selectedEgress = "eg4"
      val userAnswers = userAnswersWithTeam.set(ProduceApiSelectEgressPage, selectedEgress).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(allEgresses))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiSelectEgressRoute)
        val view = fixture.application.injector.instanceOf[ProduceApiSelectEgressView]
        val viewModel = ProduceApiSelectEgressViewModel(
          "myApis.produce.selectegress.title",
          teamEgresses,
          controllers.myapis.produce.routes.ProduceApiSelectEgressController.onSubmit(NormalMode)
        )

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(ProduceApiSelectEgressFormViewModel(selectedEgress, false)), viewModel, FakeUser)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the correct next page when valid data is submitted" in {
      val selectedEgress = "eg4"
      val userAnswers = userAnswersWithTeam.set(ProduceApiSelectEgressPage, selectedEgress).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))

      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(allEgresses))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiSelectEgressRoute)
            .withFormUrlEncodedBody(("value", selectedEgress), ("noegress", "false"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: ProduceApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[ProduceApiSessionRepository]

    val playApplication = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ProduceApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}