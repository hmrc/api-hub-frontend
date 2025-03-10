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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.hubstatus.{Feature, FeatureStatus, FrontendShutter}
import play.api.Logging
import repositories.FeatureStatusRepository

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

trait HubStatusService {

  def status(feature: Feature): Future[FeatureStatus]

  def awaitStatus(feature: Feature): FeatureStatus

  def shutterDown(feature: Feature, shutterMessage: String): Future[FeatureStatus]

  def shutterUp(feature: Feature): Future[FeatureStatus]

}

@Singleton
class HubStatusServiceImpl @Inject()(
  config: FrontendAppConfig,
  repository: FeatureStatusRepository
)(implicit ec: ExecutionContext) extends HubStatusService with Logging {

  override def status(feature: Feature): Future[FeatureStatus] = {
    repository.get(feature).flatMap {
      case Some(featureStatus) if config.shuttered && !featureStatus.shuttered => useConfig(feature)
      case Some(featureStatus) => Future.successful(featureStatus)
      case None => useConfig(feature)
    }
  }

  private def useConfig(feature: Feature): Future[FeatureStatus] = {
    val featureStatus = feature match {
      case FrontendShutter =>
        FeatureStatus(
          feature = FrontendShutter,
          shuttered = config.shuttered,
          shutterMessage = Option.when(config.shuttered)(config.shutterMessage)
        )
    }

    repository.upsert(featureStatus).map(_ => featureStatus)
  }

  override def awaitStatus(feature: Feature): FeatureStatus = {
    Await.result(status(feature), Duration(config.hubStatusTimeoutSeconds, TimeUnit.SECONDS))
  }

  override def shutterDown(feature: Feature, shutterMessage: String): Future[FeatureStatus] = {
    logger.warn(s"Shuttering feature $feature")

    val featureStatus = FeatureStatus(
      feature = feature,
      shuttered = true,
      shutterMessage = Some(shutterMessage)
    )

    repository.upsert(featureStatus)
      .map(_ => featureStatus)
  }

  override def shutterUp(feature: Feature): Future[FeatureStatus] = {
    logger.warn(s"Un-shuttering feature $feature")

    val featureStatus = FeatureStatus(
      feature = feature,
      shuttered = false,
      shutterMessage = None
    )

    repository.upsert(featureStatus)
      .map(_ => featureStatus)
  }

}
