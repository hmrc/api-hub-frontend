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

package config

import controllers.actions.*
import play.api.inject.{Binding, bind as bindz}
import play.api.{Configuration, Environment}
import services.{HubStatusService, HubStatusServiceImpl}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import java.time.{Clock, ZoneOffset}
import scala.collection.immutable.Seq

class Module extends play.api.inject.Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[?]] = {

    val bindings = Seq(
      bindz(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).eagerly(),
      bindz(classOf[AddAnApiDataRetrievalAction]).to(classOf[AddAnApiDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[AccessRequestDataRetrievalAction]).to(classOf[AccessRequestDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[CreateTeamDataRetrievalAction]).to(classOf[CreateTeamDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[ProduceApiDataRetrievalAction]).to(classOf[ProduceApiDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[UpdateApiDataRetrievalAction]).to(classOf[UpdateApiDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[AddEgressToTeamDataRetrievalAction]).to(classOf[AddEgressToTeamDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[CancelAccessRequestDataRetrievalAction]).to(classOf[CancelAccessRequestDataRetrievalActionImpl]).eagerly(),
      bindz(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).eagerly(),
      bindz[ApplicationAuthActionProvider].to(classOf[ApplicationAuthActionProviderImpl]).eagerly(),
      bindz[ApiAuthActionProvider].to(classOf[ApiAuthActionProviderImpl]).eagerly(),
      bindz[TeamAuthActionProvider].to(classOf[TeamAuthActionProviderImpl]).eagerly(),
      bindz[AddAnApiCheckContextActionProvider].to(classOf[AddAnApiCheckContextActionProviderImpl]).eagerly(),
      bindz(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).eagerly(),
      bindz(classOf[OptionalIdentifierAction]).to(classOf[OptionallyAuthenticatedIdentifierAction]),
      bindz(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC)),
      bindz[Encrypter & Decrypter].toProvider[CryptoProvider],
      bindz[Domains].to(classOf[DomainsImpl]).eagerly(),
      bindz[Hods].to(classOf[HodsImpl]).eagerly(),
      bindz[Platforms].to(classOf[PlatformsImpl]).eagerly(),
      bindz[EmailDomains].to(classOf[EmailDomainsImpl]).eagerly(),
      bindz[HipEnvironments].to(classOf[HipEnvironmentsImpl]).eagerly(),
      bindz[HubStatusService].to(classOf[HubStatusServiceImpl]).eagerly()
    )

    val authTokenInitialiserBindings: Seq[Binding[?]] = if (configuration.get[Boolean]("create-internal-auth-token-on-start")) {
        Seq(bindz(classOf[InternalAuthTokenInitialiser]).to(classOf[InternalAuthTokenInitialiserImpl]).eagerly())
    } else {
        Seq(bindz(classOf[InternalAuthTokenInitialiser]).to(classOf[NoOpInternalAuthTokenInitialiser]).eagerly())
    }

    bindings ++ authTokenInitialiserBindings
  }

}
