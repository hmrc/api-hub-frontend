package repositories

import scala.concurrent.ExecutionContext.Implicits.global

class AccessRequestSessionRepositorySpec extends UserAnswersRepositoryBehaviours {

  protected override val repository = new AccessRequestSessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock,
    cryptoProvider = cryptoProvider
  )

  "AccessRequestSessionRepository" - {
    behave like userAnswersRepository(repository)
  }

}
