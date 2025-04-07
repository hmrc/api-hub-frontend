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
import controllers.actions.{FakeApiDetail, FakeUser, FakeUserNotTeamMember}
import controllers.routes
import models.UserAnswers
import models.api.ApiGeneration.V1
import models.api.{Alpha, ApiDetail}
import models.application.TeamMember
import models.deployment.{DeploymentDetails, EgressMapping}
import models.myapis.produce.{ProduceApiDomainSubdomain, ProduceApiEgressPrefixes}
import models.team.Team
import models.user.UserModel
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atMostOnce, verify, when}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import pages.QuestionPage
import pages.myapis.update.*
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Writes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.UpdateApiSessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.ErrorTemplate

import java.time.*
import scala.concurrent.Future

class UpdateApiStartControllerSpec extends SpecBase with MockitoSugar with TableDrivenPropertyChecks with HtmlValidation {

  import UpdateApiStartControllerSpec.*

  "UpdateApiStartController" - {
    "must initiate user answers with the existing deployment details" in {
      val emptyDeploymentDetails = DeploymentDetails(
        description = None,
        status = None,
        domain = None,
        subDomain = None,
        hods = None,
        egressMappings = None,
        prefixesToRemove = None,
        egress = None,
      )
      def initialUserAnswers(deploymentDetails: DeploymentDetails) = UserAnswers(
          id = FakeUser.userId,
          lastUpdated = clock.instant()
      ).set(UpdateApiApiPage, FakeApiDetail).success.value

      def setAnswer[A](userAnswers: UserAnswers, page: QuestionPage[A], value: A)(implicit w: Writes[A]) =
        userAnswers.set(page, value).success.value

      val deploymentDetailsWithDescription = emptyDeploymentDetails
        .copy(description = Some("Description"))
      val answersWithDescription = setAnswer(
        initialUserAnswers(deploymentDetailsWithDescription),
        UpdateApiShortDescriptionPage,
        "Description"
      )

      val deploymentDetailsWithStatus = emptyDeploymentDetails
        .copy(status = Some("alpha"))
      val answersWithStatus = setAnswer(
        initialUserAnswers(deploymentDetailsWithStatus),
        UpdateApiStatusPage,
        Alpha
      )

      val deploymentDetailsWithDomainSubdomain = emptyDeploymentDetails
        .copy(
          domain = Some("domain"),
          subDomain = Some("subDomain")
        )
      val answersWithDomainSubdomain = setAnswer(
        initialUserAnswers(deploymentDetailsWithDomainSubdomain),
        UpdateApiDomainPage,
        ProduceApiDomainSubdomain("domain", "subDomain")
      )

      val deploymentDetailsWithHod = emptyDeploymentDetails
        .copy(
          hods = Some(Seq("hod")),
        )
      val answersWithHod = setAnswer(
        initialUserAnswers(deploymentDetailsWithHod),
        UpdateApiHodPage,
        Set("hod")
      )

      val deploymentDetailsWithPrefixes = emptyDeploymentDetails
        .copy(
          prefixesToRemove = Some(Seq("test-prefix-1", "test-prefix-2")),
          egressMappings = Some(Seq(EgressMapping("prefix", "egress-prefix"))),
        )
      val answersWithPrefixes = setAnswer(
        setAnswer(
          setAnswer(
            initialUserAnswers(deploymentDetailsWithPrefixes),
            UpdateApiAddPrefixesPage,
            true
          ),
            UpdateApiEgressPrefixesPage,
            ProduceApiEgressPrefixes(
              prefixes = Seq("test-prefix-1","test-prefix-2"),
              mappings = Seq("egress-prefix->prefix")
            )
        ), UpdateApiEgressAvailabilityPage,
        false
      )
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email)))

      forAll(Table(
        ("deploymentDetails", "expectedAnswers"),
        (emptyDeploymentDetails, initialUserAnswers(emptyDeploymentDetails)),
        (deploymentDetailsWithDescription, answersWithDescription),
        (deploymentDetailsWithStatus, answersWithStatus),
        (deploymentDetailsWithDomainSubdomain, answersWithDomainSubdomain),
        (deploymentDetailsWithHod, answersWithHod),
        (deploymentDetailsWithPrefixes, answersWithPrefixes)
      )) { case (deploymentDetails: DeploymentDetails, expectedUserAnswers: UserAnswers) =>
        val fixture = buildFixture()
        when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
        when(fixture.apiHubService.getDeploymentDetails(any)(any))
          .thenReturn(Future.successful(Some(deploymentDetails)))
        when(fixture.apiHubService.findTeamById(any)(any))
          .thenReturn(Future.successful(Some(team)))

        running(fixture.application) {
          val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
          val result = route(fixture.application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(nextPage.url)
          verify(fixture.sessionRepository, atMostOnce()).set(eqTo(expectedUserAnswers))
        }
      }
    }

    "must redirect to the next page" in {
      val fixture = buildFixture()
      val team = Team("test-team-id", "test-team-name", LocalDateTime.now(), Seq(TeamMember(FakeUser.email)))
      when(fixture.apiHubService.getDeploymentDetails(any)(any))
        .thenReturn(Future.successful(Some(DeploymentDetails(
          description = None,
          status = None,
          domain = None,
          subDomain = None,
          hods = None,
          egressMappings = None,
          prefixesToRemove = None,
          egress = None,
        ))))
      when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))
      when(fixture.apiHubService.findTeamById(any)(any))
        .thenReturn(Future.successful(Some(team)))

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(nextPage.url)
      }
    }

    "must return a 404 if there are no API details" in {
      val fixture = buildFixture(maybeApiDetail = None)
      val view = fixture.application.injector.instanceOf[ErrorTemplate]

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND

        contentAsString(result) mustBe
          view(
            "Page not found - 404",
            "API not found",
            s"Cannot find an API with ID id.",
            Some(FakeUser)
          )(request, messages(fixture.application))
            .toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must return a 404 if there are no deployment details" in {
      val fixture = buildFixture()
      val view = fixture.application.injector.instanceOf[ErrorTemplate]
      when(fixture.apiHubService.getDeploymentDetails(any)(any))
        .thenReturn(Future.successful(None))

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
        val result = route(fixture.application, request).value

        status(result) mustBe NOT_FOUND


        contentAsString(result) mustBe view(
          "Page not found - 404",
          "API not found",
          s"The API ${FakeApiDetail.title} has not been deployed to HIP.",
          Some(FakeUser)
        )(request, messages(fixture.application)).toString()

        contentAsString(result) must validateAsHtml
      }
    }

    "must redirect to the unauthorised page if the user is not part of the API team" in {
      val fixture = buildFixture()
      when(fixture.apiHubService.findTeams(any)(any))
        .thenReturn(Future.successful(Seq(
          Team("otherTeamId", "name", LocalDateTime.now(clock), Seq.empty)
        )))

      running(fixture.application) {
        val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
        val result = route(fixture.application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
      }
    }

    "must show a not found screen when the API is not Hub maintainable" in {
      forAll(Table(
        "deploymentDetails",
        None,
        Some(V1)
      )) { apiGeneration =>
        val fixture = buildFixture(
          maybeApiDetail = Some(FakeApiDetail.copy(apiGeneration = None)),
        )
        when(fixture.apiHubService.getDeploymentDetails(any)(any))
          .thenReturn(Future.successful(Some(DeploymentDetails(
            description = None,
            status = None,
            domain = None,
            subDomain = None,
            hods = None,
            egressMappings = None,
            prefixesToRemove = None,
            egress = None,
          ))))
        when(fixture.sessionRepository.set(any())).thenReturn(Future.successful(true))

        running(fixture.application) {
          val request = FakeRequest(controllers.myapis.update.routes.UpdateApiStartController.startProduceApi("id"))
          val result = route(fixture.application, request).value

          val view = fixture.application.injector.instanceOf[ErrorTemplate]

          status(result) mustBe NOT_FOUND
          contentAsString(result) mustBe view.apply(
              "Page not found - 404",
              "This page can’t be found",
              message = "This API is not maintainable by The Integration Hub",
              Some(FakeUser)
            )(request, messages(fixture.application))
            .toString()
          contentAsString(result) must validateAsHtml
        }
      }
    }
  }

  private case class Fixture(
    application: Application,
    sessionRepository: UpdateApiSessionRepository,
    apiHubService: ApiHubService
  )

  private def buildFixture(
                            maybeApiDetail: Option[ApiDetail] = Some(FakeApiDetail),
                            user: UserModel = FakeUser
                          ): Fixture = {
    val sessionRepository = mock[UpdateApiSessionRepository]
    val apiHubService = mock[ApiHubService]
    when(apiHubService.getApiDetail(any)(any))
      .thenReturn(Future.successful(maybeApiDetail))
    when(apiHubService.findTeams(any)(any))
      .thenReturn(Future.successful(Seq(
        Team("teamId", "name", LocalDateTime.now(clock), Seq(TeamMember(FakeUser.email)))
      )))

    val application = applicationBuilder(user = user)
      .overrides(
        bind[UpdateApiSessionRepository].toInstance(sessionRepository),
        bind[Navigator].toInstance(new FakeNavigator(nextPage)),
        bind[Clock].toInstance(clock),
        bind[ApiHubService].toInstance(apiHubService),
      )
      .build()

    Fixture(application, sessionRepository, apiHubService)
  }

}

object UpdateApiStartControllerSpec {

  private val nextPage = controllers.routes.IndexController.onPageLoad
  private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

}
