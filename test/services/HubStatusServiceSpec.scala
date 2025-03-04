/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import config.FrontendAppConfig
import models.hubstatus.{FeatureStatus, FrontendShutter}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import repositories.FeatureStatusRepository

import scala.concurrent.Future

class HubStatusServiceSpec extends AsyncFreeSpec with Matchers with MockitoSugar {

  import HubStatusServiceSpec.*

  "status" - {
    "must return the status from the repository if present" in {
      val fixture = buildFixture(false)
      val expected = FeatureStatus(FrontendShutter, false, None)

      when(fixture.repository.get(eqTo(FrontendShutter))).thenReturn(Future.successful(Some(expected)))

      fixture.service.status(FrontendShutter).map {
        result =>
          result mustBe expected
      }
    }

    "must use config as a default when not present, saving the result" in {
      val fixture = buildFixture(true)
      val expected = FeatureStatus(FrontendShutter, true, Some(configShutterMessage))

      when(fixture.repository.get(eqTo(FrontendShutter))).thenReturn(Future.successful(None))
      when(fixture.repository.upsert(any)).thenReturn(Future.successful(()))

      fixture.service.status(FrontendShutter).map {
        result =>
          verify(fixture.repository).upsert(eqTo(expected))
          result mustBe expected
      }
    }

    "must override from config when config says shuttered, saving the result" in {
      val fixture = buildFixture(true)
      val featureStatus = FeatureStatus(FrontendShutter, false, None)
      val expected = FeatureStatus(FrontendShutter, true, Some(configShutterMessage))

      when(fixture.repository.get(eqTo(FrontendShutter))).thenReturn(Future.successful(Some(featureStatus)))
      when(fixture.repository.upsert(any)).thenReturn(Future.successful(()))

      fixture.service.status(FrontendShutter).map {
        result =>
          verify(fixture.repository).upsert(eqTo(expected))
          result mustBe expected
      }
    }
  }

  "shutterDown" - {
    "must shutter the requested feature and save this" - {
      val fixture = buildFixture(false)
      val expected = FeatureStatus(FrontendShutter, true, Some(customShutterMessage))

      when(fixture.repository.upsert(any)).thenReturn(Future.successful(()))

      fixture.service.shutterDown(FrontendShutter, customShutterMessage).map {
        result =>
          verify(fixture.repository).upsert(eqTo(expected))
          result mustBe expected
      }
    }
  }

  "shutterUp" - {
    "must un-shutter the requested feature and save this" - {
      val fixture = buildFixture(false)
      val expected = FeatureStatus(FrontendShutter, false, None)

      when(fixture.repository.upsert(any)).thenReturn(Future.successful(()))

      fixture.service.shutterUp(FrontendShutter).map {
        result =>
          verify(fixture.repository).upsert(eqTo(expected))
          result mustBe expected
      }
    }
  }

  def buildFixture(configShuttered: Boolean): Fixture = {
    val devSettings = Map(
      "hubStatus.shuttered" -> configShuttered.toString,
      "hubStatus.shutterMessage" -> configShutterMessage
    )

    val config = new FrontendAppConfig(Configuration.load(Environment.simple(), devSettings))
    val repository = mock[FeatureStatusRepository]
    val service = new HubStatusServiceImpl(config, repository)

    Fixture(config, repository, service)
  }

}

private object HubStatusServiceSpec {

  val configShutterMessage = "test-config-shutter-message"
  val customShutterMessage = "test-custom-shutter-message"

  case class Fixture(
    config: FrontendAppConfig,
    repository: FeatureStatusRepository,
    service: HubStatusService
  )

}
