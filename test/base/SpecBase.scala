/*
 * Copyright 2023 HM Revenue & Customs
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

package base

import config.*
import controllers.actions.*
import fakes.{FakeDomains, FakeEmailDomains, FakeHipEnvironments, FakeHods, FakeHubStatusService, FakePlatforms}
import models.UserAnswers
import models.user.UserModel
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.{Application, Configuration}
import services.HubStatusService

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  val userAnswersId: String = "id"

  def emptyUserAnswers : UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None,
                                   user: UserModel = FakeUser,
                                   testConfiguration: Configuration = Configuration.empty): GuiceApplicationBuilder =
    GuiceApplicationBuilder(configuration = testConfiguration)
      .overrides(
        bind[UserModel].toInstance(user),
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[AccessRequestDataRetrievalAction].toInstance(new FakeAccessRequestDataRetrievalAction(userAnswers)),
        bind[AddAnApiDataRetrievalAction].toInstance(new FakeAddAnApiDataRetrievalAction(userAnswers)),
        bind[CreateTeamDataRetrievalAction].toInstance(new FakeCreateTeamDataRetrievalAction(userAnswers)),
        bind[AddEgressToTeamDataRetrievalAction].toInstance(new FakeAddEgressToTeamDataRetrievalAction(userAnswers)),
        bind[ProduceApiDataRetrievalAction].toInstance(new FakeProduceApiDataRetrievalAction(userAnswers)),
        bind[UpdateApiDataRetrievalAction].toInstance(new FakeUpdateApiDataRetrievalAction(userAnswers)),
        bind[CancelAccessRequestDataRetrievalAction].toInstance(new FakeCancelAccessRequestDataRetrievalAction(userAnswers)),
        bind[Domains].toInstance(FakeDomains),
        bind[Hods].toInstance(FakeHods),
        bind[EmailDomains].toInstance(FakeEmailDomains),
        bind[Platforms].toInstance(FakePlatforms),
        bind[HipEnvironments].toInstance(FakeHipEnvironments),
        bind[HubStatusService].toInstance(FakeHubStatusService)
  )

}
