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

package controllers.myapis.produce

import base.SpecBase
import controllers.actions.FakeUser
import forms.myapis.produce.ProduceApiChooseEgressFormProvider
import generators.EgressGenerator
import models.myapis.produce.{ProduceApiChooseEgress,ProduceApiEgressPrefixes}
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any,argThat}
import org.mockito.Mockito.{when,verify}
import org.scalatestplus.mockito.MockitoSugar
import pages.myapis.produce.{ProduceApiEgressPrefixesPage,ProduceApiChooseEgressPage}
import play.api.Application as PlayApplication
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.myapis.produce.ProduceApiEgressView

import scala.concurrent.Future

class ProduceApiChooseEgressControllerSpec extends SpecBase with MockitoSugar with EgressGenerator with HtmlValidation {

  private def onwardRoute = Call("GET", "/foo")

  private val formProvider = new ProduceApiChooseEgressFormProvider()
  private val form = formProvider()

  private lazy val produceApiChooseEgressRoute = controllers.myapis.produce.routes.ProduceApiEgressController.onPageLoad(NormalMode).url

  "ProduceApiChooseEgress Controller" - {

    "must return OK and the correct view for a GET" in {

      val egressGateways = sampleEgressGateways()
      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.apiHubService.listEgressGateways()(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiChooseEgressRoute)

        val result = route(fixture.application, request).value

        val view = fixture.application.injector.instanceOf[ProduceApiEgressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, FakeUser, "http://localhost:8490/guides/integration-hub-guide", egressGateways )(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must populate the view correctly on a GET when the questions have been previously answered" in {

      val egressGateways = sampleEgressGateways()

      val chooseEgress = ProduceApiChooseEgress(Some(egressGateways.head.id), true)
      val userAnswers = UserAnswers(userAnswersId).set(ProduceApiChooseEgressPage, chooseEgress).success.value

      val fixture = buildFixture(userAnswers = Some(userAnswers))
      when(fixture.apiHubService.listEgressGateways()(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiChooseEgressRoute)

        val view = fixture.application.injector.instanceOf[ProduceApiEgressView]

        val result = route(fixture.application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(chooseEgress), NormalMode, FakeUser, "http://localhost:8490/guides/integration-hub-guide", egressGateways)(request, messages(fixture.application)).toString
        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the correct next page when valid data is submitted" in {
      val egressGateways = sampleEgressGateways()

      val fixture = buildFixture(userAnswers = Some(emptyUserAnswers))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      when(fixture.apiHubService.listEgressGateways()(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseEgressRoute)
            .withFormUrlEncodedBody(("selectEgress", egressGateways.head.id),("egressPrefix", "true"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must remove Egress Prefixes if user changes answer from Yes to No" in {
      val egressGateways = sampleEgressGateways()

      val userAnswersWithPrefixesAndAnswerNo = emptyUserAnswers
        .set(ProduceApiEgressPrefixesPage, ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/existing->/replacement"))).success.value
        .set(ProduceApiChooseEgressPage, ProduceApiChooseEgress(Some(egressGateways.head.id), false))
        .success.value
      val fixture = buildFixture(userAnswers = Some(userAnswersWithPrefixesAndAnswerNo))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      when(fixture.apiHubService.listEgressGateways()(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseEgressRoute)
            .withFormUrlEncodedBody(("selectEgress", egressGateways.head.id),("egressPrefix", "false"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(fixture.sessionRepository).set(argThat((userAnswers: UserAnswers) => userAnswers.get(ProduceApiEgressPrefixesPage).isEmpty))
      }
    }

    "must not remove Egress Prefixes if user answer remains Yes" in {
      val egressGateways = sampleEgressGateways()
      val previousPrefixesAnswer = ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/existing->/replacement"))

      val userAnswersWithPrefixesAndAnswerYes = emptyUserAnswers
        .set(ProduceApiEgressPrefixesPage, previousPrefixesAnswer).success.value
        .set(ProduceApiChooseEgressPage, ProduceApiChooseEgress(Some(egressGateways.head.id), true))
        .success.value
      val fixture = buildFixture(userAnswers = Some(userAnswersWithPrefixesAndAnswerYes))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

      when(fixture.apiHubService.listEgressGateways()(any)).thenReturn(Future.successful(egressGateways))

      running(fixture.application) {
        val request =
          FakeRequest(POST, produceApiChooseEgressRoute)
            .withFormUrlEncodedBody(("selectEgress", egressGateways.head.id), ("egressPrefix", "true"))

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(fixture.sessionRepository).set(argThat((userAnswers: UserAnswers) => userAnswers.get(ProduceApiEgressPrefixesPage).contains(previousPrefixesAnswer)))
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
