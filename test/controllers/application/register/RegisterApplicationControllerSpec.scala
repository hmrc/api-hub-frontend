package controllers.application.register

import base.SpecBase
import controllers.actions.FakeUser
import generators.{ApplicationGenerator, TeamGenerator}
import models.{CheckMode, UserAnswers}
import models.application.{Creator, NewApplication}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import pages.application.register.{RegisterApplicationNamePage, RegisterApplicationTeamPage}
import play.api.inject.bind
import play.api.{Application => PlayApplication}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ApiHubService
import utils.HtmlValidation
import views.html.application.register.RegisterApplicationSuccessView

import scala.concurrent.Future

class RegisterApplicationControllerSpec extends SpecBase with MockitoSugar with ArgumentMatchersSugar with HtmlValidation with ApplicationGenerator with TeamGenerator {

  "register" - {
    "must register an application and return the success view" in {
      val application = sampleApplication()
      val team = sampleTeam()

      val userAnswers = UserAnswers(userAnswersId)
        .set(RegisterApplicationNamePage, application.name).success.value
        .set(RegisterApplicationTeamPage, team).success.value

      val fixture = buildFixture(userAnswers)

      when(fixture.apiHubService.registerApplication(any)(any)).thenReturn(Future.successful(application))
      when(fixture.sessionRepository.clear(any)).thenReturn(Future.successful(true))

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.register.routes.RegisterApplicationController.register())
        val result = route(fixture.playApplication, request).value
        val view = fixture.playApplication.injector.instanceOf[RegisterApplicationSuccessView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(application, FakeUser)(request, messages(fixture.playApplication)).toString()
        contentAsString(result) must validateAsHtml

        val newApplication = NewApplication(application.name, Creator(FakeUser.email.value), team.id)
        verify(fixture.apiHubService).registerApplication(eqTo(newApplication))(any)
        verify(fixture.sessionRepository).clear(eqTo(userAnswersId))
      }
    }

    "must redirect to the name page if the answer is missing" in {
      val team = sampleTeam()

      val userAnswers = UserAnswers(userAnswersId)
        .set(RegisterApplicationTeamPage, team).success.value

      val fixture = buildFixture(userAnswers)

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.register.routes.RegisterApplicationController.register())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.application.register.routes.RegisterApplicationNameController.onPageLoad(CheckMode).url)

        verifyZeroInteractions(fixture.apiHubService)
        verifyZeroInteractions(fixture.sessionRepository)
      }
    }

    "must redirect to the select team page if the answer is missing" in {
      val application = sampleApplication()

      val userAnswers = UserAnswers(userAnswersId)
        .set(RegisterApplicationNamePage, application.name).success.value

      val fixture = buildFixture(userAnswers)

      running(fixture.playApplication) {
        val request = FakeRequest(controllers.application.register.routes.RegisterApplicationController.register())
        val result = route(fixture.playApplication, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.application.register.routes.RegisterApplicationTeamController.onPageLoad(CheckMode).url)

        verifyZeroInteractions(fixture.apiHubService)
        verifyZeroInteractions(fixture.sessionRepository)
      }
    }
  }

  private case class Fixture(
    playApplication: PlayApplication,
    apiHubService: ApiHubService,
    sessionRepository: SessionRepository
  )

  private def buildFixture(userAnswers: UserAnswers): Fixture = {
    val apiHubService = mock[ApiHubService]
    val sessionRepository = mock[SessionRepository]

    val playApplication = applicationBuilder(Some(userAnswers))
      .overrides(
        bind[ApiHubService].toInstance(apiHubService),
        bind[SessionRepository].toInstance(sessionRepository)
      )
      .build()

    Fixture(playApplication, apiHubService, sessionRepository)
  }

}
