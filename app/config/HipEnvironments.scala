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

package config

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.{Format, Json, Reads}
import services.ApiHubService
import uk.gov.hmrc.http.HeaderCarrier

import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration

trait AbstractHipEnvironment[T] {
  def id: String
  def rank: Int
  def isProductionLike: Boolean
  def promoteTo: Option[T]
}

case class BaseHipEnvironment(
                               id: String,
                               rank: Int,
                               isProductionLike: Boolean,
                               promoteTo: Option[String]
                             ) extends AbstractHipEnvironment[String]

object BaseHipEnvironment {
  implicit val formatBaseHipEnvironment: Format[BaseHipEnvironment] = Json.format[BaseHipEnvironment]
}

trait HipEnvironment extends AbstractHipEnvironment[HipEnvironment] {
  def nameKey(implicit messages: play.api.i18n.Messages): String = messages(s"site.environment.$id")
}

case class DefaultHipEnvironment(
                                  id: String,
                                  rank: Int,
                                  isProductionLike: Boolean,
                                  promoteTo: Option[HipEnvironment]) extends HipEnvironment

case class ShareableHipConfig(
                               environments: Seq[BaseHipEnvironment],
                               production: String,
                               deployTo: String
                             )

object ShareableHipConfig {
  implicit val formatShareableHipConfig: Format[ShareableHipConfig] = Json.format[ShareableHipConfig]
}

trait HipEnvironments {

  protected def baseEnvironments: Seq[BaseHipEnvironment]

  def environments: Seq[HipEnvironment] = baseEnvironments
    .map(
      base =>
        new Object with HipEnvironment:
          override val id: String = base.id
          override val rank: Int = base.rank
          override val isProductionLike: Boolean = base.isProductionLike
          override lazy val promoteTo: Option[HipEnvironment] = base.promoteTo.map(forId)
    )
    .sortBy(_.rank)

  def forId(environmentId: String): HipEnvironment = {
    environments
      .find(_.id == environmentId)
      .getOrElse(throw new IllegalArgumentException(s"No configuration for environment $environmentId"))
  }

  def forEnvironmentIdOptional(environmentId: String): Option[HipEnvironment] = {
    environments.find(_.id == environmentId)
  }

  def forUrlPathParameter(pathParameter: String): HipEnvironment =
    environments.find(hipEnvironment => hipEnvironment.id == pathParameter)
      .getOrElse(throw new IllegalArgumentException(s"No configuration for environment $pathParameter"))

  def production: HipEnvironment

  def deployTo: HipEnvironment

}

@Singleton
class HipEnvironmentsImpl @Inject(apiHubService: ApiHubService) extends HipEnvironments {

  private lazy val config = Await.result(apiHubService.listEnvironments()(HeaderCarrier()), Duration(10, TimeUnit.SECONDS))
  
  override protected def baseEnvironments: Seq[BaseHipEnvironment] = config.environments

  override def production: HipEnvironment = forId(config.production)

  override def deployTo: HipEnvironment = forId(config.deployTo)
}