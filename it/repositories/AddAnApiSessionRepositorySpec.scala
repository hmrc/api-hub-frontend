package repositories

import scala.concurrent.ExecutionContext.Implicits.global

class AddAnApiSessionRepositorySpec extends UserAnswersRepositoryBehaviours {

  protected override val repository = new AddAnApiSessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock,
    cryptoProvider = cryptoProvider
  )

  "AddAnApiSessionRepository" - {
    behave like userAnswersRepository(repository)
  }

}
