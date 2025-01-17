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
import controllers.myapis.produce.routes as produceApiRoutes
import controllers.routes
import fakes.{FakeDomains, FakeHods}
import models.api.Alpha
import models.deployment.*
import models.myapis.produce.*
import models.team.Team
import models.user.{Permissions, UserModel}
import models.{CheckMode, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.QuestionPage
import pages.myapis.produce.*
import play.api.Application as PlayApplication
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.ProduceApiSessionRepository
import services.ApiHubService
import viewmodels.checkAnswers.myapis.produce.*
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.myapis.produce.ProduceApiCheckYourAnswersViewModel
import views.html.myapis.DeploymentSuccessView
import views.html.myapis.produce.{ProduceApiCheckYourAnswersView, ProduceApiDeploymentErrorView}

import java.time.LocalDateTime
import scala.concurrent.Future

class ProduceApiCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private lazy val produceApiCheckYourAnswersRoute = controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onPageLoad().url

  private val fullyPopulatedUserAnswers = UserAnswers(userAnswersId)
    .set(ProduceApiChooseTeamPage, Team("id", "name", LocalDateTime.now(), Seq.empty)).success.value
    .set(ProduceApiEnterOasPage, "oas").success.value
    .set(ProduceApiEnterApiTitlePage, "api name").success.value
    .set(ProduceApiShortDescriptionPage, "api description").success.value
    .set(ProduceApiEgressPrefixesPage, ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/existing->/replacement"))).success.value
    .set(ProduceApiHodPage, Set("hod1")).success.value
    .set(ProduceApiDomainPage, ProduceApiDomainSubdomain("domain", "subdomain")).success.value
    .set(ProduceApiStatusPage, Alpha).success.value
    .set(ProduceApiEgressSelectionPage, "egress").success.value
    .set(ProduceApiEgressAvailabilityPage, true).success.value
    .set(ProduceApiPassthroughPage, true).success.value

  private def summaryList()(implicit msg: Messages) = SummaryListViewModel(Seq(
    ProduceApiChooseTeamSummary.row(fullyPopulatedUserAnswers),
    ProduceApiEnterOasSummary.row(fullyPopulatedUserAnswers),
    ProduceApiNameSummary.row(fullyPopulatedUserAnswers),
    ProduceApiShortDescriptionSummary.row(fullyPopulatedUserAnswers),
    ProduceApiEgressAvailabilitySummary.row(fullyPopulatedUserAnswers),
    ProduceApiEgressSummary.row(fullyPopulatedUserAnswers),
    ProduceApiEgressPrefixesSummary.row(fullyPopulatedUserAnswers),
    ProduceApiHodSummary.row(fullyPopulatedUserAnswers, FakeHods),
    ProduceApiDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
    ProduceApiSubDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
    ProduceApiStatusSummary.row(fullyPopulatedUserAnswers),
    ProduceApiPassthroughSummary.row(fullyPopulatedUserAnswers)
  ).flatten)

  "ProduceApiCheckYourAnswersController" - {
    "must return OK and the correct view for a GET" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      implicit val msgs: Messages = messages(fixture.application)
      
      running(fixture.application) {
        val request = FakeRequest(GET, produceApiCheckYourAnswersRoute)
        val result = route(fixture.application, request).value
        val viewModel = ProduceApiCheckYourAnswersViewModel(
          controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onSubmit()
        )
        val view = fixture.application.injector.instanceOf[ProduceApiCheckYourAnswersView]
        val expectedSummaryList = summaryList()
        
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(expectedSummaryList, FakeUser, viewModel)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, produceApiCheckYourAnswersRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return 200 and the deployment success view on a successful POST" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response: DeploymentsResponse = SuccessfulDeploymentsResponse("id", "1.0.0", 1, "uri.com")
      val view = fixture.application.injector.instanceOf[DeploymentSuccessView]

      when(fixture.apiHubService.generateDeployment(any)(any))
        .thenReturn(Future.successful(response))

      when(fixture.sessionRepository.clear(FakeUser.userId)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, produceApiCheckYourAnswersRoute)

        val result = route(fixture.application, request).value

        val successRoute = controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onSuccess("api name", "id").url

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(successRoute)

        val successPageRequest = FakeRequest(GET, successRoute)

        val successResult =  route(fixture.application, successPageRequest).value

        contentAsString(successResult) mustEqual view(FakeUser, "id", "api name")(successPageRequest, messages(fixture.application)).toString
        verify(fixture.sessionRepository).clear(FakeUser.userId)
      }
    }

    "must return Bad Request and the deployment error view for an unsuccessful POST" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response = InvalidOasResponse(
        FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      )
      val view = fixture.application.injector.instanceOf[ProduceApiDeploymentErrorView]

      when(fixture.apiHubService.generateDeployment(any)(any))
        .thenReturn(Future.successful[DeploymentsResponse](response))

      running(fixture.application) {
        val request = FakeRequest(POST, produceApiCheckYourAnswersRoute)
        implicit val msgs: Messages = messages(fixture.application)

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(FakeUser, response.failure)(request, messages(fixture.application)).toString
      }
    }

    "must clear the user answers and redirect to the index page on cancel" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response = InvalidOasResponse(
        FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      )
      val view = fixture.application.injector.instanceOf[ProduceApiDeploymentErrorView]

      when(fixture.apiHubService.generateDeployment(any)(any))
        .thenReturn(Future.successful[DeploymentsResponse](response))

      when(fixture.sessionRepository.clear(FakeUser.userId)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, controllers.myapis.produce.routes.ProduceApiCheckYourAnswersController.onCancel().url)
        implicit val msgs: Messages = messages(fixture.application)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.IndexController.onPageLoad.url)
        verify(fixture.sessionRepository).clear(FakeUser.userId)
      }
    }

    "must redirect to a recovery page when a user answer is missing on a POST submission" in {
      val nonSupportUser = FakeUser
      val supportUser = nonSupportUser.copy(permissions = Permissions(true, true, true))
      val response: DeploymentsResponse = SuccessfulDeploymentsResponse("id", "1.0.0", 1, "uri.com")

      forAll(Table(
        ("userAnswerToRemove", "user", "expectedLocation"),
        (ProduceApiChooseTeamPage, nonSupportUser, produceApiRoutes.ProduceApiChooseTeamController.onPageLoad(CheckMode).url),
        (ProduceApiEnterOasPage, nonSupportUser, produceApiRoutes.ProduceApiEnterOasController.onPageLoad(CheckMode).url),
        (ProduceApiEnterApiTitlePage, nonSupportUser, routes.JourneyRecoveryController.onPageLoad().url),
        (ProduceApiShortDescriptionPage, nonSupportUser, produceApiRoutes.ProduceApiShortDescriptionController.onPageLoad(CheckMode).url),
        (ProduceApiEgressPrefixesPage, nonSupportUser, produceApiRoutes.ProduceApiEgressPrefixesController.onPageLoad(CheckMode).url),
        (ProduceApiHodPage, nonSupportUser, produceApiRoutes.ProduceApiHodController.onPageLoad(CheckMode).url),
        (ProduceApiDomainPage, nonSupportUser, produceApiRoutes.ProduceApiDomainController.onPageLoad(CheckMode).url),
        (ProduceApiStatusPage, nonSupportUser, produceApiRoutes.ProduceApiStatusController.onPageLoad(CheckMode).url),
        (ProduceApiPassthroughPage, supportUser, produceApiRoutes.ProduceApiPassthroughController.onPageLoad(CheckMode).url),
        (ProduceApiEgressSelectionPage, supportUser, produceApiRoutes.ProduceApiEgressSelectionController.onPageLoad(CheckMode).url),
        (ProduceApiEgressAvailabilityPage, supportUser, produceApiRoutes.ProduceApiEgressAvailabilityController.onPageLoad(CheckMode).url),
      )){ case (userAnswerToRemove: QuestionPage[?], user: UserModel, expectedLocation: String) =>
        val fixture = buildFixture(
          Some(fullyPopulatedUserAnswers.remove(userAnswerToRemove).get),
          Some(user),
        )

        when(fixture.apiHubService.generateDeployment(any)(any))
          .thenReturn(Future.successful(response))

        val request = FakeRequest(POST, produceApiCheckYourAnswersRoute)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedLocation
      }
    }

    "formatAsKebabCase" - {
      forAll(Table(
        "text",
        ("API title", "api-title"),
        (" API title ", "api-title"),
        (" API    title ", "api-title"),
        (" API_title ", "api-title"),
        ("A/(P)I &*^((&^ ti£*^&%tle", "api-title"),
        ("API title 1", "api-title-1"),
      )) { case (text, expected) =>
        s"must transform $text into $expected" in {
          ProduceApiCheckYourAnswersController.formatAsKebabCase(text) mustBe expected
        }
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: ProduceApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers], userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[ProduceApiSessionRepository]
    val playApplication = applicationBuilder(userAnswers, userModel.getOrElse(FakeUser))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ProduceApiSessionRepository].toInstance(sessionRepository)
      )
      .build()
    Fixture(playApplication, apiHubService, sessionRepository)
  }
}
