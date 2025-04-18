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

package repositories

import scala.concurrent.ExecutionContext.Implicits.global

class AddEgressToTeamSessionRepositorySpec extends UserAnswersRepositoryBehaviours {

  protected override val repository = new AddEgressToTeamSessionRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock,
    cryptoProvider = cryptoProvider
  )

  "AddEgressToTeamSessionRepository" - {
    behave like userAnswersRepository(repository.asInstanceOf[UserAnswersRepository])
  }

}
