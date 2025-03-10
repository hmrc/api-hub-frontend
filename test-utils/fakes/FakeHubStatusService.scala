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

package fakes

import models.hubstatus.{Feature, FeatureStatus, FrontendShutter}
import services.HubStatusService

import scala.concurrent.Future

object FakeHubStatusService extends HubStatusService {

  val frontendShutterStatus: FeatureStatus = FeatureStatus(FrontendShutter, false, None)

  override def status(feature: Feature): Future[FeatureStatus] = {
    feature match {
      case FrontendShutter => Future.successful(frontendShutterStatus)
    }
  }

  override def awaitStatus(feature: Feature): FeatureStatus = {
    frontendShutterStatus
  }

  override def shutterDown(feature: Feature, shutterMessage: String): Future[FeatureStatus] = {
    feature match {
      case FrontendShutter => Future.successful(FeatureStatus(FrontendShutter, true, Some(shutterMessage)))
    }
  }

  override def shutterUp(feature: Feature): Future[FeatureStatus] = {
    feature match {
      case FrontendShutter => Future.successful(FeatureStatus(FrontendShutter, false, None))
    }
  }

}
