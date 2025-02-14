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

package controllers.myapis.update

import base.SpecBase
import controllers.actions.FakeUser
import fakes.FakeHipEnvironments
import forms.myapis.produce.ProduceApiEgressSelectionForm
import generators.EgressGenerator
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.update.UpdateApiEgressSelectionPage
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import viewmodels.myapis.produce.ProduceApiEgressSelectionViewModel
import views.html.myapis.produce.ProduceApiEgressSelectionView

import scala.concurrent.Future

class UpdateApiEgressSelectionControllerSpec extends SpecBase with MockitoSugar with EgressGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = ProduceApiEgressSelectionForm()
  private val form = formProvider()

  private lazy val updateApiEgressSelectionRoute = controllers.myapis.update.routes.UpdateApiEgressSelectionController.onPageLoad(NormalMode).url

  "UpdateApiEgressSelection Controller" - {

    "must return OK and the correct view for a GET" in {

      val egressGateways = sampleEgressGateways()
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEgressSelectionRoute)

        val result = route(fixture.application, request).value

        val viewModel = ProduceApiEgressSelectionViewModel(
          "myApis.update.selectegress.title",
          controllers.myapis.update.routes.UpdateApiEgressSelectionController.onSubmit(NormalMode),
          controllers.myapis.update.routes.UpdateApiEgressAvailabilityController.onPageLoad(NormalMode).url,
          false
        )
        val view = fixture.application.injector.instanceOf[ProduceApiEgressSelectionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, FakeUser, "http://localhost:8490/guides/integration-hub-guide", egressGateways, viewModel)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the questions have been previously answered" in {

      val egressGateways = sampleEgressGateways()

      val chooseEgress = egressGateways.head.id
      val userAnswers = UserAnswers(userAnswersId).set(UpdateApiEgressSelectionPage, chooseEgress).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))
      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiEgressSelectionRoute)

        val viewModel = ProduceApiEgressSelectionViewModel(
          "myApis.update.selectegress.title",
          controllers.myapis.update.routes.UpdateApiEgressSelectionController.onSubmit(NormalMode),
          controllers.myapis.update.routes.UpdateApiEgressAvailabilityController.onPageLoad(NormalMode).url,
          false
        )
        val view = fixture.application.injector.instanceOf[ProduceApiEgressSelectionView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(chooseEgress), FakeUser, "http://localhost:8490/guides/integration-hub-guide", egressGateways, viewModel)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the correct next page when valid data is submitted" in {
      val egressGateways = sampleEgressGateways()

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      when(fixture.apiHubService.listEgressGateways(eqTo(FakeHipEnvironments.deployTo))(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request =
          FakeRequest(POST, updateApiEgressSelectionRoute)
            .withFormUrlEncodedBody(("selectEgress", egressGateways.head.id))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: UpdateApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers]): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[UpdateApiSessionRepository]

    val playApplication = applicationBuilder(userAnswers)
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[UpdateApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}