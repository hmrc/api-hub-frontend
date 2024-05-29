package controllers.deployment

import base.SpecBase
import connectors.ApplicationsConnector
import controllers.actions.{FakeApiDetail, FakeUser}
import controllers.deployment.SimpleApiRedeploymentController.RedeploymentRequestFormProvider
import models.application.TeamMember
import models.deployment.{RedeploymentRequest, SuccessfulDeploymentsResponse}
import models.team.Team
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application => PlayApplication}
import services.ApiHubService
import utils.HtmlValidation
import views.html.deployment.{DeploymentSuccessView, SimpleApiRedeploymentView}

import java.time.LocalDateTime
import scala.concurrent.Future

class SimpleApiRedeploymentControllerSpec
  extends SpecBase
    with Matchers
    with MockitoSugar
    with ArgumentMatchersSugar
    with HtmlValidation {

  import SimpleApiRedeploymentControllerSpec._

  "onPageLoad" - {
    "must return 200 Ok and the correct view" in {
      val fixture = buildFixture()

      mockApiAndTeam(fixture)

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.deployment.routes.SimpleApiRedeploymentController.onPageLoad(FakeApiDetail.id))
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[SimpleApiRedeploymentView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, FakeApiDetail, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml
      }
    }
  }

  "onSubmit" - {
    "must respond with 200 OK and a success view response when success returned by APIM" in {
      val fixture = buildFixture()

      val response = SuccessfulDeploymentsResponse(
        id = "test-id",
        version = "test-version",
        mergeRequestIid = 101,
        uri = "test-uri"
      )

      mockApiAndTeam(fixture)

      when(fixture.applicationsConnector.updateDeployment(any, any)(any)).thenReturn(Future.successful(Some(response)))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.deployment.routes.SimpleApiRedeploymentController.onSubmit(FakeApiDetail.id))
          .withFormUrlEncodedBody(validForm: _*)
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[DeploymentSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(FakeUser, response)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        verify(fixture.applicationsConnector).updateDeployment(eqTo(FakeApiDetail.publisherReference), eqTo(redeploymentRequest))(any)
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    applicationsConnector: ApplicationsConnector
  )

  private def buildFixture(): Fixture = {
    val apiHubService = mock[ApiHubService]
    val applicationsConnector = mock[ApplicationsConnector]
    val playApplication = applicationBuilder()
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[ApplicationsConnector].toInstance(applicationsConnector)
      )
      .build()
    Fixture(playApplication, apiHubService, applicationsConnector)
  }

  private def mockApiAndTeam(fixture: Fixture): Unit = {
    when(fixture.apiHubService.getApiDetail(eqTo(FakeApiDetail.id))(any)).thenReturn(Future.successful(Some(FakeApiDetail)))
    when(fixture.apiHubService.findTeams(eqTo(FakeUser.email))(any)).thenReturn(Future.successful(userTeams))
  }

}

object SimpleApiRedeploymentControllerSpec extends OptionValues {

  private val form = new RedeploymentRequestFormProvider()()

  private lazy val owningTeam = Team(
    id = FakeApiDetail.teamId.value,
    name = "test-team-name",
    created = LocalDateTime.now(),
    teamMembers = Seq(
      TeamMember(FakeUser.email.value)
    )
  )

  private lazy val userTeams: Seq[Team] = Seq(owningTeam)

  private val redeploymentRequest = RedeploymentRequest(
    description = "test-description",
    oas = "test-oas",
    status = "test-status"
  )

  private val validForm = Seq(
    "description" -> redeploymentRequest.description,
    "oas" -> redeploymentRequest.oas,
    "status" -> redeploymentRequest.status
  )

}
