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

import config.{Domains, EmailDomains, HipEnvironments, Hods, Platforms}
import controllers.actions.{FakeOptionalIdentifierAction, OptionalIdentifierAction, OptionalUserProvider, OptionalUserProviderImpl}
import fakes.{FakeDomains, FakeEmailDomains, FakeHipEnvironments, FakeHods, FakeHubStatusService, FakePlatforms}
import models.user.UserModel
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import services.HubStatusService

trait OptionallyAuthenticatedSpecBase extends AnyFreeSpec with Matchers {

  def applicationBuilder(user: Option[UserModel]): GuiceApplicationBuilder = {
    GuiceApplicationBuilder()
      .overrides(
        bind[OptionalUserProvider].toInstance(new OptionalUserProviderImpl(user)),
        bind[OptionalIdentifierAction].to[FakeOptionalIdentifierAction],
        bind[Domains].toInstance(FakeDomains),
        bind[Hods].toInstance(FakeHods),
        bind[Platforms].toInstance(FakePlatforms),
        bind[EmailDomains].toInstance(FakeEmailDomains),
        bind[HipEnvironments].toInstance(FakeHipEnvironments),
        bind[HubStatusService].toInstance(FakeHubStatusService)
      )
  }

  def messages(application: Application): Messages = {
    application.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  }

}
