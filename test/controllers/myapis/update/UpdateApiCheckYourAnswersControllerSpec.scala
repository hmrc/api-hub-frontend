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

package controllers.myapis.update

import base.SpecBase
import controllers.actions.{FakeApiDetail, FakeSupporter, FakeUser}
import controllers.myapis.update.routes as updateApiRoutes
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
import pages.myapis.update.*
import play.api.Application as PlayApplication
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.{SessionRepository, UpdateApiSessionRepository}
import services.ApiHubService
import viewmodels.checkAnswers.myapis.update.*
import viewmodels.govuk.all.SummaryListViewModel
import viewmodels.myapis.produce
import viewmodels.myapis.produce.{ProduceApiCheckYourAnswersViewModel, ProduceApiDeploymentErrorViewModel}
import views.html.ErrorTemplate
import views.html.myapis.DeploymentSuccessView
import views.html.myapis.produce.{ProduceApiCheckYourAnswersView, ProduceApiDeploymentErrorView}

import java.time.LocalDateTime
import scala.concurrent.Future

class UpdateApiCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks {

  private lazy val updateApiCheckYourAnswersRoute = controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onPageLoad()
  private lazy val updateApiCancelRoute = controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onCancel()
  private lazy val errorViewModel = produce.ProduceApiDeploymentErrorViewModel(updateApiCancelRoute, updateApiCheckYourAnswersRoute)

  private val fullyPopulatedUserAnswers = UserAnswers(userAnswersId)
    .set(UpdateApiApiPage, FakeApiDetail).success.value
    .set(UpdateApiEnterOasPage, "oas").success.value
    .set(UpdateApiEnterApiTitlePage, "title").success.value
    .set(UpdateApiShortDescriptionPage, "api description").success.value
    .set(UpdateApiEgressPrefixesPage, ProduceApiEgressPrefixes(Seq("/prefix"), Seq("/existing->/replacement"))).success.value
    .set(UpdateApiHodPage, Set("hod1")).success.value
    .set(UpdateApiDomainPage, ProduceApiDomainSubdomain("domain", "subdomain")).success.value
    .set(UpdateApiStatusPage, Alpha).success.value
    .set(UpdateApiEgressSelectionPage, "egress").success.value
    .set(UpdateApiEgressAvailabilityPage, true).success.value

  private def summaryList()(implicit msg: Messages) = SummaryListViewModel(Seq(
    UpdateApiEnterOasSummary.row(fullyPopulatedUserAnswers),
    UpdateApiNameSummary.row(fullyPopulatedUserAnswers),
    UpdateApiShortDescriptionSummary.row(fullyPopulatedUserAnswers),
    UpdateApiEgressAvailabilitySummary.row(fullyPopulatedUserAnswers),
    UpdateApiEgressSummary.row(fullyPopulatedUserAnswers),
    UpdateApiEgressPrefixesSummary.row(fullyPopulatedUserAnswers),
    UpdateApiHodSummary.row(fullyPopulatedUserAnswers, FakeHods),
    UpdateApiDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
    UpdateApiSubDomainSummary.row(fullyPopulatedUserAnswers, FakeDomains),
  ).flatten)

  "UpdateApiCheckYourAnswersController" - {
    "must return OK and the correct view for a GET for a non-support user" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      implicit val msgs: Messages = messages(fixture.application)
      
      running(fixture.application) {
        val request = FakeRequest(GET, updateApiCheckYourAnswersRoute.url)
        val result = route(fixture.application, request).value
        val viewModel = ProduceApiCheckYourAnswersViewModel(
          controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onSubmit()
        )
        val view = fixture.application.injector.instanceOf[ProduceApiCheckYourAnswersView]
        val expectedSummaryList = summaryList()
        
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(expectedSummaryList, FakeUser, viewModel)(request, messages(fixture.application)).toString
      }
    }
    
    "must return OK and the correct view for a GET for a support user" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers), userModel = Some(FakeSupporter))
      implicit val msgs: Messages = messages(fixture.application)
      
      running(fixture.application) {
        val request = FakeRequest(GET, updateApiCheckYourAnswersRoute)
        val result = route(fixture.application, request).value
        val viewModel = ProduceApiCheckYourAnswersViewModel(
          controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onSubmit()
        )
        val view = fixture.application.injector.instanceOf[ProduceApiCheckYourAnswersView]
        val expectedSummaryList = summaryList().copy(rows = summaryList().rows :+ UpdateApiStatusSummary.row(fullyPopulatedUserAnswers, FakeSupporter).get)
        
        status(result) mustEqual OK

        contentAsString(result) mustEqual view(expectedSummaryList, FakeSupporter, viewModel)(request, messages(fixture.application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val fixture = buildFixture(None)

      running(fixture.application) {
        val request = FakeRequest(GET, updateApiCheckYourAnswersRoute.url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return 200 and the deployment success view on a successful POST" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response: DeploymentsResponse = SuccessfulDeploymentsResponse("id", "1.0.0", 1, "uri.com")
      val view = fixture.application.injector.instanceOf[DeploymentSuccessView]

      when(fixture.apiHubService.updateDeployment(any, any)(any))
        .thenReturn(Future.successful(Some(response)))

      when(fixture.sessionRepository.clear(FakeUser.userId)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, updateApiCheckYourAnswersRoute.url)

        val result = route(fixture.application, request).value

        val successRoute = controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onSuccess("title", "pubRef").url

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(successRoute)

        val successPageRequest = FakeRequest(GET, successRoute)

        val successResult =  route(fixture.application, successPageRequest).value

        contentAsString(successResult) mustEqual view(FakeUser, "pubRef", "title")(successPageRequest, messages(fixture.application)).toString
        verify(fixture.sessionRepository).clear(FakeUser.userId)
      }
    }

    "must return Bad Request and the deployment error view for an unsuccessful POST" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response = InvalidOasResponse(
        FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      )
      val view = fixture.application.injector.instanceOf[ProduceApiDeploymentErrorView]

      when(fixture.apiHubService.updateDeployment(any, any)(any))
        .thenReturn(Future.successful[Option[DeploymentsResponse]](Some(response)))

      running(fixture.application) {
        val request = FakeRequest(POST, updateApiCheckYourAnswersRoute.url)
        implicit val msgs: Messages = messages(fixture.application)

        val result = route(fixture.application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(FakeUser, response.failure, errorViewModel)(request, messages(fixture.application)).toString
      }
    }

    "must return Not Found for a POST with no response" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response = InvalidOasResponse(
        FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      )
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      when(fixture.apiHubService.updateDeployment(any, any)(any))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(POST, updateApiCheckYourAnswersRoute.url)
        implicit val msgs: Messages = messages(fixture.application)

        val result = route(fixture.application, request).value

        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            "Cannot find an API with ID pubRef.",
            Some(FakeUser)
          )(request, msgs)
            .toString()
      }
    }

    "must clear the user answers and redirect to the index page on cancel" in {
      val fixture = buildFixture(Some(fullyPopulatedUserAnswers))
      val response = InvalidOasResponse(
        FailuresResponse("code", "reason", Some(Seq(Error("type", "message"))))
      )
      val view = fixture.application.injector.instanceOf[ProduceApiDeploymentErrorView]

      when(fixture.apiHubService.updateDeployment(any, any)(any))
        .thenReturn(Future.successful[Option[DeploymentsResponse]](Some(response)))

      when(fixture.sessionRepository.clear(FakeUser.userId)).thenReturn(Future.successful(true))

      running(fixture.application) {
        val request = FakeRequest(POST, controllers.myapis.update.routes.UpdateApiCheckYourAnswersController.onCancel().url)
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
        (UpdateApiEnterOasPage, nonSupportUser, updateApiRoutes.UpdateApiEnterOasController.onPageLoad(CheckMode).url),
        (UpdateApiShortDescriptionPage, nonSupportUser, updateApiRoutes.UpdateApiShortDescriptionController.onPageLoad(CheckMode).url),
        (UpdateApiEgressPrefixesPage, nonSupportUser, updateApiRoutes.UpdateApiEgressPrefixesController.onPageLoad(CheckMode).url),
        (UpdateApiHodPage, nonSupportUser, updateApiRoutes.UpdateApiHodController.onPageLoad(CheckMode).url),
        (UpdateApiDomainPage, nonSupportUser, updateApiRoutes.UpdateApiDomainController.onPageLoad(CheckMode).url),
        (UpdateApiEgressSelectionPage, supportUser, updateApiRoutes.UpdateApiEgressSelectionController.onPageLoad(CheckMode).url),
        (UpdateApiEgressAvailabilityPage, supportUser, updateApiRoutes.UpdateApiEgressAvailabilityController.onPageLoad(CheckMode).url),
      )){ case (userAnswerToRemove: QuestionPage[?], user: UserModel, expectedLocation: String) =>
        val fixture = buildFixture(
          Some(fullyPopulatedUserAnswers.remove(userAnswerToRemove).get),
          Some(user),
        )

        when(fixture.apiHubService.updateDeployment(any, any)(any))
          .thenReturn(Future.successful(Some(response)))

        val request = FakeRequest(POST, updateApiCheckYourAnswersRoute.url)

        val result = route(fixture.application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedLocation
      }
    }
  }

  private case class Fixture(
                              application: PlayApplication,
                              apiHubService: ApiHubService,
                              sessionRepository: UpdateApiSessionRepository
                            )

  private def buildFixture(userAnswers: Option[UserAnswers], userModel: Option[UserModel] = None): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[UpdateApiSessionRepository]
    val playApplication = applicationBuilder(userAnswers, userModel.getOrElse(FakeUser))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[UpdateApiSessionRepository].toInstance(sessionRepository)
      )
      .build()
    Fixture(playApplication, apiHubService, sessionRepository)
  }
}
